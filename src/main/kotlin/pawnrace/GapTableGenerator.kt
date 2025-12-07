package pawnrace

/**
 * Offline tool to precompute a gap table.
 *
 * For each (whiteGap, blackGap) pair:
 *  - White plays the best first move (worst for Black),
 *  - From that position we run a depth-limited search assuming best play,
 *  - We record the resulting score from Black's perspective.
 *
 * Output is written to "gap_table.txt" in descending score order.
 */
fun main() {
    val results = mutableListOf<Pair<String, Int>>()

    // Compute all 64 gap-pair scores
    for (wFileChar in 'a'..'h') {
        for (bFileChar in 'a'..'h') {
            val score = evaluateGapPairForBlack(wFileChar, bFileChar)
            val key = "${wFileChar.uppercaseChar()}${bFileChar.uppercaseChar()}"
            results += key to score
        }
    }

    // Sort descending by score (best for Black first)
    val sorted = results.sortedByDescending { it.second }

    // Write output
    val outFile = java.io.File("gap_table.txt")
    outFile.printWriter().use { out ->
        //out.println("# whiteGap blackGap scoreForBlack (sorted descending)")

        for ((key, _) in sorted) {
            //out.println("$key $score")
            out.println(key)
        }
    }

    println("gap_table.txt generated and sorted.")
}

/**
 * Evaluate a single (whiteGap, blackGap) pair from Black's perspective.
 *
 * White is assumed to choose the first move that is *worst for Black*
 * (minimises Black's eventual value), according to a depth-limited search.
 */
private fun evaluateGapPairForBlack(wGapChar: Char, bGapChar: Char): Int {
    val board = Board(File(wGapChar), File(bGapChar))
    val game0 = Game(board, Piece.W)

    val blackAI = Player(Piece.B)

    val whiteMoves = game0.moves(Piece.W)
    if (whiteMoves.isEmpty()) {
        // Degenerate position; just evaluate from Black's POV
        return blackAI.evaluate(game0)
    }

    var worstForBlack = Int.MAX_VALUE

    for (wm in whiteMoves) {
        val game1 = game0.copy()
        game1.applyMove(wm)  // White plays wm, now Black to move

        // Depth-limited search from this position.
        val depth = 4  // change this to 5 or 6 if you want slower but stronger offline computation
        val replyScore = searchForBlack(game1, blackAI, depth)

        if (replyScore < worstForBlack) {
            worstForBlack = replyScore
        }
    }

    return worstForBlack
}

/**
 * Depth-limited minimax search from the given position using blackAI.evaluate.
 *
 * We treat:
 *  - Black as maximiser,
 *  - White as minimiser.
 *
 * No time control here: this is an offline tool.
 */
private fun searchForBlack(
    game: Game,
    blackAI: Player,
    depth: Int
): Int {
    if (depth == 0 || game.over()) {
        return blackAI.evaluate(game)
    }

    val side = game.player
    val moves = game.moves(side)
    if (moves.isEmpty()) {
        return blackAI.evaluate(game)
    }

    return if (side == Piece.B) {
        // Maximiser
        var best = Int.MIN_VALUE
        for (m in moves) {
            game.applyMove(m)
            val s = searchForBlack(game, blackAI, depth - 1)
            game.unapplyMove()
            if (s > best) best = s
        }
        best
    } else {
        // Minimiser
        var best = Int.MAX_VALUE
        for (m in moves) {
            game.applyMove(m)
            val s = searchForBlack(game, blackAI, depth - 1)
            game.unapplyMove()
            if (s < best) best = s
        }
        best
    }
}
