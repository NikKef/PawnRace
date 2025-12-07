package pawnrace

import kotlin.math.abs

class Board(whiteGap: File, blackGap: File) {
    companion object {
        // the default file and rank size of a chess board
        const val SIZE = 8

        // the rank where the white pawns are set up
        private const val WHITE_RANK_START = 1

        // the rank where the black pawns are set up
        private const val BLACK_RANK_START = 6

        // ASCII code of uppercase A
        private const val ASCII_A_UP = 'A'.code

        fun startRank(piece: Piece): Int =
            if (piece == Piece.W) WHITE_RANK_START else BLACK_RANK_START
    }

    val board: Array<Array<Piece?>> = Array(SIZE) { r ->
        Array(SIZE) { f ->
            when {
                r == WHITE_RANK_START && f != whiteGap.getInt() -> Piece.W
                r == BLACK_RANK_START && f != blackGap.getInt() -> Piece.B
                else -> null
            }
        }
    }

    fun pieceAt(pos: Position): Piece? =
        board[pos.rank.pos][pos.file.getInt()]

    fun positionsOf(piece: Piece): List<Position> {
        val result = mutableListOf<Position>()

        for (r in 0 until SIZE)
            for (f in 0 until SIZE)
                if (board[r][f] == piece)
                    result.add(Position(File.fromInt(f), Rank(r)))

        return result
    }

    fun isValidMove(move: Move, lastMove: Move? = null): Boolean {
        val fromRank = move.from.rank.pos
        val toRank = move.to.rank.pos
        val fromFile = move.from.file.getInt()
        val toFile = move.to.file.getInt()

        // correct piece on from-square
        if (board[fromRank][fromFile] != move.piece) return false

        val d = move.piece.direction
        val s = startRank(move.piece)

        return when (move.type) {
            MoveType.PEACEFUL -> {
                if (fromFile != toFile) return false

                // single step
                if (fromRank + d == toRank &&
                    board[toRank][toFile] == null
                ) return true

                // double step
                if (fromRank == s &&
                    toRank == s + 2 * d &&
                    board[fromRank + d][fromFile] == null &&
                    board[toRank][toFile] == null
                ) return true

                false
            }

            MoveType.CAPTURE -> {
                board[toRank][toFile] == move.piece.getOpposite() &&
                        fromRank + d == toRank &&
                        abs(fromFile - toFile) == 1
            }

            MoveType.EN_PASSANT -> {
                if (lastMove == null || lastMove.type != MoveType.PEACEFUL)
                    return false

                val lastFromRank = lastMove.from.rank.pos
                val lastToRank = lastMove.to.rank.pos
                val lastFile = lastMove.to.file.getInt()
                val ls = startRank(lastMove.piece)
                val ld = lastMove.piece.direction

                // last move must be 2-step from its start rank
                if (lastFromRank != ls) return false
                if (lastToRank != ls + 2 * ld) return false

                // standing next to that pawn
                if (fromRank != lastToRank) return false
                if (fromRank + d != toRank) return false
                if (abs(fromFile - toFile) != 1) return false
                if (toFile != lastFile) return false

                true
            }
        }
    }

    fun move(m: Move): Board {
        board[m.from.rank.pos][m.from.file.getInt()] = null
        board[m.to.rank.pos][m.to.file.getInt()] = m.piece

        if (m.type == MoveType.EN_PASSANT)
            board[m.to.rank.pos - m.piece.direction][m.to.file.getInt()] = null

        return this
    }

    fun unMove(m: Move): Board {
        board[m.from.rank.pos][m.from.file.getInt()] = m.piece

        when (m.type) {
            MoveType.PEACEFUL -> board[m.to.rank.pos][m.to.file.getInt()] =
                null

            MoveType.CAPTURE -> board[m.to.rank.pos][m.to.file.getInt()] =
                m.piece.getOpposite()

            else -> {
                board[m.to.rank.pos][m.to.file.getInt()] = null
                board[m.to.rank.pos - m.piece.direction][m.to.file.getInt()] =
                    m.piece.getOpposite()
            }
        }

        return this
    }

    override fun toString(): String {
        val res = StringBuilder()
        val filesLine = StringBuilder()

        filesLine.append("    ")
        for (f in 0 until SIZE)
            filesLine.append("${(f + ASCII_A_UP).toChar()} ")
        filesLine.append("\n")

        res.append(filesLine)
        res.append("\n")

        for (r in SIZE - 1 downTo 0) {
            res.append("${(r + 1).digitToChar()}   ")

            for (f in 0 until SIZE) {
                if (board[r][f] == null)
                    res.append(". ")
                else
                    res.append("${board[r][f]} ")
            }

            res.append("   ${(r + 1).digitToChar()}\n")
        }

        res.append("\n")
        res.append(filesLine)

        return res.toString()
    }

    fun copy(): Board {
        val newBoard = Board(
            File.fromInt(0),
            File.fromInt(0)
        )
        for (r in 0 until SIZE) {
            for (f in 0 until SIZE) {
                newBoard.board[r][f] = this.board[r][f]
            }
        }
        return newBoard
    }

}
