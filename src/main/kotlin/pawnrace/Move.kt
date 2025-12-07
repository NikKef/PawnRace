package pawnrace

data class Move(
    val piece: Piece,
    val from: Position,
    val to: Position,
    val type: MoveType
) {
    override fun toString(): String =
        when (type) {
            MoveType.PEACEFUL -> "$to"
            else -> "${from.file}x$to"
        }
}
