package pawnrace

enum class Piece(val direction: Int) {
    B(-1) {
        override fun getOpposite() = W
    },
    W(1) {
        override fun getOpposite() = B
    };

    abstract fun getOpposite(): Piece
}

enum class MoveType {
    PEACEFUL,
    CAPTURE,
    EN_PASSANT
}

data class File(val pos: Char) {
    init {
        require(
            pos in 'a'..'h'
        ) {
            "Invalid file: $pos"
        }
    }

    companion object {
        // the ascii code of lowercase 'a'
        const val ASCII_A = 'a'.code

        fun fromInt(i: Int): File {
            require(i in 0..7) { "Invalid file index: $i" }
            return File((ASCII_A + i).toChar())
        }
    }

    override fun toString(): String =
        pos.toString()

    fun getInt(): Int =
        pos.code - ASCII_A
}

data class Rank(val pos: Int) {
    init {
        require(pos in 0..7) { "Invalid rank index: $pos" }
    }

    companion object {
        fun fromChar(c: Char): Rank {
            require(c in '1'..'8') { "Invalid rank char: $c" }
            val d = c.digitToInt()
            return Rank(d - 1)
        }
    }

    override fun toString(): String =
        (pos + 1).toString()
}

data class Position(val file: File, val rank: Rank) {

    override fun toString(): String = "$file$rank"
}
