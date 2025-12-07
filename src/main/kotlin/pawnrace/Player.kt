package pawnrace

import java.util.concurrent.Executors
import java.util.concurrent.ExecutionException

class Player(val piece: Piece, var opponent: Player? = null) {
    companion object {
        // Total allowed threads according to spec
        private val allowedThreads: Int =
            Runtime.getRuntime().availableProcessors() / 2 - 1

        // Worker threads only. Leave room for main thread and any runner overhead
        private val poolSize: Int = (allowedThreads - 1).coerceAtLeast(0)

        private val executor =
            if (poolSize > 0)
                Executors.newFixedThreadPool(poolSize)
            else
                null
    }

    private object TimeUpException : RuntimeException()

    private val TIME_LIMIT_NANOS = 4_950_000_000L

    private fun checkDeadline(deadline: Long) {
        if (System.nanoTime() >= deadline) throw TimeUpException
    }

    private data class RootResult(val move: Move, val score: Int)

    data class MoveEval(val move: Move, val score: Int)

    fun getAllPawns(game: Game): List<Position> =
        game.board.positionsOf(piece)

    fun getAllValidMoves(game: Game): List<Move> =
        game.moves(piece)

    fun isPassedPawn(pos: Position, game: Game, p: Piece = piece): Boolean {
        val board = game.board
        if (board.pieceAt(pos) != p) return false

        val d = p.direction
        val startFile = pos.file.getInt()
        val opp = p.getOpposite()

        var currentRank = pos.rank.pos + d
        while (currentRank in 0 until Board.SIZE) {
            for (df in -1..1) {
                val currentFile = df + startFile
                if (currentFile in 0 until Board.SIZE)
                    if (board.pieceAt(
                            Position(
                                File.fromInt(currentFile),
                                Rank(currentRank)
                            )
                        ) == opp
                    )
                        return false
            }

            currentRank += d
        }

        return true
    }

    // Top level move selection with iterative deepening and root parallelism
    fun makeMove(game: Game): Move? {
        val moves = getAllValidMoves(game)
        if (moves.isEmpty()) return null

        // Fast tactic: immediate winning move
        for (m in moves) {
            game.applyMove(m)
            if (game.over() && game.winner() == piece) {
                return m
            }
            game.unapplyMove()
        }

        val startTime = System.nanoTime()
        val deadline = startTime + TIME_LIMIT_NANOS

        var bestMove: Move = moves.random()   // fallback
        var depth = 1

        try {
            while (true) {
                val preferred = if (depth > 1) bestMove else null
                val result = searchRoot(game, moves, depth, deadline, preferred)
                bestMove = result.move

                depth += 1
                checkDeadline(deadline)
            }
        } catch (e: TimeUpException) {
            // [DEBUG OUTPUT]
            // val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
            // println("AI ($piece): time up at depth $depth, using bestMove " +
            //        "from previous depth (elapsed ${elapsedMs}ms)")
        }

        game.applyMove(bestMove)
        return bestMove
    }

    // Order root moves by a quick static evaluation after playing them,
    // and put the previous best move first if present.
    private fun orderRootMoves(
        game: Game,
        rootMoves: List<Move>,
        deadline: Long,
        preferred: Move?
    ): List<Move> {
        if (rootMoves.size <= 1) return rootMoves

        val scored = ArrayList<Pair<Move, Int>>(rootMoves.size)

        for (m in rootMoves) {
            checkDeadline(deadline)
            game.applyMove(m)
            val score = evaluate(game)
            game.unapplyMove()
            scored.add(m to score)
        }

        // Higher scores are better for this player
        val ordered = scored
            .sortedByDescending { it.second }
            .map { it.first }
            .toMutableList()

        // If we have a preferred move from the previous depth,
        // move it to the front if it is in the list.
        if (preferred != null) {
            val idx = ordered.indexOf(preferred)
            if (idx > 0) {
                val m = ordered.removeAt(idx)
                ordered.add(0, m)
            }
        }

        return ordered
    }

    // Decide whether to parallelise or run single threaded
    private fun searchRoot(
        game: Game,
        rootMoves: List<Move>,
        depth: Int,
        deadline: Long,
        preferred: Move? = null
    ): RootResult {
        // First, order moves by static eval and previous best move
        val orderedMoves = orderRootMoves(game, rootMoves, deadline, preferred)

        // If no pool or depth small or few moves, do single threaded
        if (executor == null || depth == 1 || orderedMoves.size < 3) {
            // [DEBUG OUTPUT]
            // println("AI ($piece): depth $depth, searching ${orderedMoves.size} root moves single-threaded")
            return searchRootSingleThread(game, orderedMoves, depth, deadline)
        }

        // [DEBUG OUTPUT]
        // println("AI ($piece): depth $depth, searching ${orderedMoves.size} root moves in parallel")
        val futures = orderedMoves.map { move ->
            executor.submit<MoveEval> {
                checkDeadline(deadline)
                val gameCopy = game.copy()
                gameCopy.applyMove(move)
                val score = search(
                    gameCopy,
                    depth - 1,
                    Int.MIN_VALUE + 1,
                    Int.MAX_VALUE,
                    deadline
                )
                MoveEval(move, score)
            }
        }

        var bestMove: Move = orderedMoves[0]
        var bestScore = Int.MIN_VALUE

        for (future in futures) {
            try {
                checkDeadline(deadline)
                val result = future.get()
                val score = result.score
                if (score > bestScore) {
                    bestScore = score
                    bestMove = result.move
                }
            } catch (e: ExecutionException) {
                if (e.cause is TimeUpException) {
                    throw TimeUpException
                } else {
                    throw e
                }
            }
        }

        return RootResult(bestMove, bestScore)
    }

    private fun searchRootSingleThread(
        game: Game,
        rootMoves: List<Move>,
        depth: Int,
        deadline: Long
    ): RootResult {
        var bestMove: Move = rootMoves[0]
        var bestScore = Int.MIN_VALUE
        var alpha = Int.MIN_VALUE
        val beta = Int.MAX_VALUE

        for (m in rootMoves) {
            checkDeadline(deadline)

            val gameCopy = game.copy()
            gameCopy.applyMove(m)

            val score = search(
                gameCopy,
                depth - 1,
                alpha,
                beta,
                deadline
            )

            if (score > bestScore) {
                bestScore = score
                bestMove = m
            }
            if (score > alpha) {
                alpha = score
            }
        }

        return RootResult(bestMove, bestScore)
    }

    // Core minimax, alpha beta, single threaded
    private fun search(
        game: Game,
        depth: Int,
        alpha: Int,
        beta: Int,
        deadline: Long,
        canExtend: Boolean = true
    ): Int {
        checkDeadline(deadline)

        // Immediate terminal: game already over
        if (game.over()) {
            return evaluate(game)
        }

        // Horizon: consider a small tactical extension
        if (depth == 0) {
            return if (canExtend && hasTacticalMoveForExtension(game)) {
                // Look one more ply, but do not allow further extensions down this line
                search(game, 1, alpha, beta, deadline, false)
            } else {
                evaluate(game)
            }
        }

        val sideToMove = game.player
        val moves = game.moves(sideToMove)
        if (moves.isEmpty()) {
            return evaluate(game)
        }

        val orderedMoves = orderMovesAtNode(game, moves, sideToMove)

        var a = alpha
        var b = beta

        return if (sideToMove == piece) {
            // Maximiser
            var best = Int.MIN_VALUE
            for (m in orderedMoves) {
                checkDeadline(deadline)
                game.applyMove(m)
                val score = search(game, depth - 1, a, b, deadline, canExtend)
                game.unapplyMove()

                if (score > best) best = score
                if (score > a) a = score
                if (a >= b) break
            }
            best
        } else {
            // Minimiser
            var best = Int.MAX_VALUE
            for (m in orderedMoves) {
                checkDeadline(deadline)
                game.applyMove(m)
                val score = search(game, depth - 1, a, b, deadline, canExtend)
                game.unapplyMove()

                if (score < best) best = score
                if (score < b) b = score
                if (a >= b) break
            }
            best
        }
    }

    // Decide if this node at depth 0 deserves a one-ply tactical extension
    private fun hasTacticalMoveForExtension(game: Game): Boolean {
        val side = game.player
        val moves = game.moves(side)
        if (moves.isEmpty()) return false

        val board = game.board
        val opp = side.getOpposite()

        for (m in moves) {
            if (isPromotionMove(m, side)) return true

            if (m.type != MoveType.PEACEFUL) {
                val capturedPos = when (m.type) {
                    MoveType.CAPTURE -> m.to
                    else -> {
                        val r = m.to.rank.pos - side.direction
                        val f = m.to.file.getInt()
                        Position(File.fromInt(f), Rank(r))
                    }
                }

                if (board.pieceAt(capturedPos) == opp &&
                    isPassedPawn(capturedPos, game, opp)
                ) {
                    return true
                }
            }
        }

        return false
    }

    private fun isPromotionMove(m: Move, sideToMove: Piece): Boolean {
        val toRank = m.to.rank.pos
        return when (sideToMove) {
            Piece.W -> toRank == Board.SIZE - 1
            Piece.B -> toRank == 0
        }
    }

    // Cheap move ordering for internal nodes
    private fun orderMovesAtNode(
        game: Game,
        moves: List<Move>,
        sideToMove: Piece
    ): List<Move> {
        if (moves.size <= 1) return moves

        fun isPromotion(m: Move): Boolean {
            val toRank = m.to.rank.pos
            return when (sideToMove) {
                Piece.W -> toRank == Board.SIZE - 1
                Piece.B -> toRank == 0
            }
        }

        fun isCapture(m: Move): Boolean =
            m.type != MoveType.PEACEFUL

        fun isFromPassedPawn(m: Move): Boolean =
            isPassedPawn(m.from, game, sideToMove)

        // Lower priority value means searched earlier
        fun priority(m: Move): Int =
            when {
                isPromotion(m) -> 0 // immediate promotion
                isCapture(m) -> 1 // captures (including en passant)
                isFromPassedPawn(m) -> 2 // advance of a passed pawn
                else -> 3
            }

        // Sort by priority
        return moves.sortedBy { priority(it) }
    }

    // Position evaluation from this player's perspective
    fun evaluate(game: Game): Int {
        val board = game.board

        if (game.over()) {
            return when (game.winner()) {
                // certain win
                piece -> Int.MAX_VALUE
                // certain loss
                piece.getOpposite() -> Int.MIN_VALUE + 1
                // stalemate
                else -> 0
            }
        }

        var score = 0

        for (pos in board.positionsOf(piece)) {
            score += scorePawn(piece, pos, game)
        }

        val opp = piece.getOpposite()
        for (pos in board.positionsOf(opp)) {
            score -= scorePawn(opp, pos, game)
        }

        val tempoBonus = 5
        score += if (game.player == piece) tempoBonus else -tempoBonus

        return score
    }

    private fun scorePawn(p: Piece, pos: Position, game: Game): Int {
        val rank = pos.rank.pos

        val distanceToGoal = when (p) {
            Piece.W -> Board.SIZE - 1 - rank
            Piece.B -> rank
        }

        // How far advanced this pawn is, from 1 (start rank) to 7 (promotion rank)
        val advanced = Board.SIZE - 1 - distanceToGoal

        // Base value for simply existing and being advanced
        val base = 12
        val positionScore = base * advanced * advanced

        val isPassed = isPassedPawn(pos, game, p)
        // Passed pawns get more reward the closer they are to promotion
        val passedBonus = if (isPassed) 20 * advanced * advanced else 0

        // Structure features
        val chainBonus = if (isDefendedByPawn(pos, game, p)) 25 else 0
        val isolatedPenalty = if (isIsolatedPawn(pos, game, p)) 15 else 0
        val immobilePenalty = if (isImmobilePawn(pos, game, p)) 10 else 0
        val backwardPenalty = if (isBackwardPawn(pos, game, p)) 8 else 0

        return positionScore +
                passedBonus +
                chainBonus -
                isolatedPenalty -
                immobilePenalty -
                backwardPenalty
    }

    private fun isDefendedByPawn(pos: Position, game: Game, p: Piece): Boolean {
        val board = game.board
        val rank = pos.rank.pos
        val file = pos.file.getInt()
        val d = p.direction

        val backRank = rank - d
        if (backRank !in 0 until Board.SIZE) return false

        val backRankObj = Rank(backRank)

        for (df in listOf(-1, 1)) {
            val f = file + df
            if (f in 0 until Board.SIZE) {
                val defender = board.pieceAt(
                    Position(File.fromInt(f), backRankObj)
                )
                if (defender == p) return true
            }
        }
        return false
    }

    private fun isIsolatedPawn(pos: Position, game: Game, p: Piece): Boolean {
        val board = game.board
        val file = pos.file.getInt()

        fun hasFriendOnFile(fIdx: Int): Boolean {
            if (fIdx !in 0 until Board.SIZE) return false
            for (r in 0 until Board.SIZE) {
                val pieceAt = board.pieceAt(
                    Position(File.fromInt(fIdx), Rank(r))
                )
                if (pieceAt == p) return true
            }
            return false
        }

        val hasLeftFriend = hasFriendOnFile(file - 1)
        val hasRightFriend = hasFriendOnFile(file + 1)

        return !hasLeftFriend && !hasRightFriend
    }

    private fun isImmobilePawn(pos: Position, game: Game, p: Piece): Boolean {
        val board = game.board
        val rank = pos.rank.pos
        val file = pos.file.getInt()
        val d = p.direction
        val opp = p.getOpposite()

        val frontRank = rank + d
        if (frontRank !in 0 until Board.SIZE) {
            // Already on last rank or off board, treat as not immobile here
            return false
        }

        val frontPiece = board.pieceAt(
            Position(File.fromInt(file), Rank(frontRank))
        )
        val frontBlocked = frontPiece != null

        var canCapture = false
        val targetRankObj = Rank(frontRank)

        for (df in listOf(-1, 1)) {
            val f = file + df
            if (f in 0 until Board.SIZE) {
                val target = board.pieceAt(
                    Position(File.fromInt(f), targetRankObj)
                )
                if (target == opp) {
                    canCapture = true
                    break
                }
            }
        }

        // No forward push and no capture available
        return frontBlocked && !canCapture
    }

    private fun isBackwardPawn(pos: Position, game: Game, p: Piece): Boolean {
        val board = game.board
        val rank = pos.rank.pos
        val file = pos.file.getInt()
        val d = p.direction
        val opp = p.getOpposite()

        // Passed pawns are never considered backward
        if (isPassedPawn(pos, game, p)) return false

        // Look for an opposing pawn ahead on the same file
        var blockerRank = -1
        var r = rank + d
        while (r in 0 until Board.SIZE) {
            val pieceAt = board.pieceAt(
                Position(File.fromInt(file), Rank(r))
            )
            if (pieceAt == p) return false
            if (pieceAt == opp) {
                blockerRank = r
                break
            }
            r += d
        }
        if (blockerRank == -1) return false

        // Check if there is friendly support on adjacent files in front
        fun hasSupportOnFile(fIdx: Int): Boolean {
            if (fIdx !in 0 until Board.SIZE) return false
            var rr = rank
            while (rr in 0 until Board.SIZE) {
                val pieceAt = board.pieceAt(
                    Position(File.fromInt(fIdx), Rank(rr))
                )
                if (pieceAt == p) return true
                rr += d
            }
            return false
        }

        val hasSupport =
            hasSupportOnFile(file - 1) || hasSupportOnFile(file + 1)

        return !hasSupport
    }
}