package pawnrace

import java.util.Stack

class Game(
    val board: Board,
    var player: Piece,
    val moves: Stack<Move> = Stack()
) {
    private fun lastMove(): Move? =
        if (moves.empty()) null else moves.peek()

    fun applyMove(move: Move) {
        moves.push(move)
        board.move(move)
        player = player.getOpposite()
    }

    fun unapplyMove() {
        if (moves.isNotEmpty()) {
            player = player.getOpposite()
            board.unMove(moves.pop())
        }
    }

    // Finds all possible moves for the current player
    fun moves(piece: Piece): List<Move> {
        val result = mutableListOf<Move>()
        val positions = board.positionsOf(piece)
        val last = lastMove()

        for (pos in positions) {
            // Forward 1
            moveForwardBy(pos, 1, piece)?.let { m ->
                if (board.isValidMove(m, last)) result.add(m)
            }

            // Forward 2
            if (pos.rank.pos == Board.startRank(piece)) {
                moveForwardBy(pos, 2, piece)?.let { m ->
                    if (board.isValidMove(m, last)) result.add(m)
                }
            }

            // Diagonal left capture
            moveDiagonalBy(pos, true, piece, MoveType.CAPTURE)?.let { m ->
                if (board.isValidMove(m, last)) result.add(m)
            }

            // Diagonal right capture
            moveDiagonalBy(pos, false, piece, MoveType.CAPTURE)?.let { m ->
                if (board.isValidMove(m, last)) result.add(m)
            }

            // En passant left
            moveDiagonalBy(pos, true, piece, MoveType.EN_PASSANT)?.let { m ->
                if (board.isValidMove(m, last)) result.add(m)
            }

            // En passant right
            moveDiagonalBy(pos, false, piece, MoveType.EN_PASSANT)?.let { m ->
                if (board.isValidMove(m, last)) result.add(m)
            }
        }

        return result
    }

    private fun moveForwardBy(pos: Position, step: Int, piece: Piece): Move? {
        val d = piece.direction
        val r = pos.rank.pos
        val f = pos.file.getInt()

        val toRank = r + step * d
        if (toRank !in 0 until Board.SIZE) return null

        val to = Position(File.fromInt(f), Rank(toRank))

        return Move(piece, pos, to, MoveType.PEACEFUL)
    }

    private fun moveDiagonalBy(
        pos: Position,
        isLeft: Boolean,
        piece: Piece,
        type: MoveType
    ): Move? {
        val d = piece.direction
        val r = pos.rank.pos
        val f = pos.file.getInt()

        val toRank = r + d
        if (toRank !in 0 until Board.SIZE) return null

        val toFile = if (isLeft) f - 1 else f + 1
        if (toFile !in 0 until Board.SIZE) return null

        val to = Position(File.fromInt(toFile), Rank(toRank))
        return Move(piece, pos, to, type)
    }

    fun over(): Boolean {
        if (hasPawnOnEndRank()) return true

        // If either side has no pawns left
        if (board.positionsOf(Piece.W).isEmpty()) return true
        if (board.positionsOf(Piece.B).isEmpty()) return true

        // Stalemate
        if (moves(player).isEmpty()) return true

        return false
    }


    private fun hasPawnOnEndRank(): Boolean {
        for (f in 0 until Board.SIZE) {
            val file = File.fromInt(f)
            if (board.pieceAt(Position(file, Rank(0))) == Piece.B)
                return true
            if (board.pieceAt(Position(file, Rank(Board.SIZE - 1))) == Piece.W)
                return true
        }
        return false
    }

    fun winner(): Piece? {
        val whiteHas = board.positionsOf(Piece.W).isNotEmpty()
        val blackHas = board.positionsOf(Piece.B).isNotEmpty()

        // One side has pawns, the other does not
        if (!blackHas) return Piece.W
        if (!whiteHas) return Piece.B

        // Pawn on last rank
        for (f in 0 until Board.SIZE) {
            val file = File.fromInt(f)
            if (board.pieceAt(Position(file, Rank(Board.SIZE - 1))) == Piece.W)
                return Piece.W
            if (board.pieceAt(Position(file, Rank(0))) == Piece.B)
                return Piece.B
        }

        return null
    }

    fun parseMove(san: String): Move? {
        val dir = player.direction
        val last = lastMove()

        return if (san.length == 2) {
            parsePeacefulMove(san, dir, last)
        } else {
            parseCaptureMove(san, dir, last)
        }
    }

    private fun parsePeacefulMove(san: String, dir: Int, last: Move?): Move? {
        val fileChar = san[0].lowercaseChar()
        val rankChar = san[1]

        if (fileChar !in 'a'..'h' || rankChar !in '1'..'8')
            return null

        val to = Position(File(fileChar), Rank.fromChar(rankChar))
        val toRankIdx = to.rank.pos

        // 1-step
        val fromRank1 = toRankIdx - dir
        if (fromRank1 in 0 until Board.SIZE) {
            val from1 = Position(File(fileChar), Rank(fromRank1))
            val m1 = Move(player, from1, to, MoveType.PEACEFUL)
            if (board.isValidMove(m1, last)) return m1
        }

        // 2-step
        val fromRank2 = toRankIdx - 2 * dir
        if (fromRank2 in 0 until Board.SIZE) {
            val from2 = Position(File(fileChar), Rank(fromRank2))
            val m2 = Move(player, from2, to, MoveType.PEACEFUL)
            if (board.isValidMove(m2, last)) return m2
        }

        return null
    }

    private fun parseCaptureMove(san: String, dir: Int, last: Move?): Move? {
        if (san.length != 4) return null
        if (san[1] != 'x') return null

        val fromFileChar = san[0].lowercaseChar()
        val toFileChar = san[2].lowercaseChar()
        val toRankChar = san[3]

        if (fromFileChar !in 'a'..'h' || toFileChar !in 'a'..'h') return null
        if (toRankChar !in '1'..'8') return null

        val to = Position(File(toFileChar), Rank.fromChar(toRankChar))
        val toRankIdx = to.rank.pos

        val fromRankIdx = toRankIdx - dir
        if (fromRankIdx !in 0 until Board.SIZE) return null

        val from = Position(File(fromFileChar), Rank(fromRankIdx))

        // try normal capture
        val cap = Move(player, from, to, MoveType.CAPTURE)
        if (board.isValidMove(cap, last)) return cap

        // try en passant
        val ep = Move(player, from, to, MoveType.EN_PASSANT)
        if (board.isValidMove(ep, last)) return ep

        return null
    }

    fun copy(): Game {
        val boardCopy = board.copy()
        val movesCopy = Stack<Move>()
        movesCopy.addAll(this.moves)

        return Game(
            boardCopy,
            this.player,
            movesCopy
        )
    }
}
