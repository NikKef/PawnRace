package pawnrace

import java.io.PrintWriter
import java.io.InputStreamReader
import java.io.BufferedReader

class PawnRace {
    fun playGame(colour: Char, output: PrintWriter, input: BufferedReader) {
        val aiColour =
            when (colour) {
                'W', 'w' -> Piece.W
                else -> Piece.B
            }

        val oppColour = aiColour.getOpposite()

        val aiPlayer = Player(aiColour)
        val oppPlayer = Player(oppColour, aiPlayer)

        aiPlayer.opponent = oppPlayer

        if (aiColour == Piece.B)
        {
            try {
                val stream = {}.javaClass.getResourceAsStream("/gap_table.txt")
                    ?: throw Exception()
                val reader = BufferedReader(InputStreamReader(stream))
                val lines = reader.readLines()

                reader.close()
                stream.close()

                output.println(lines[(0 until 10).random()])
            } catch (e: Exception) {
                output.print("BC")
            }
        }

        val gaps = input.readLine()

        val board = Board(
            File(gaps[0].lowercaseChar()),
            File(gaps[1].lowercaseChar()),
        )

        val game = Game(board, Piece.W)

        println(game.board)

        if (aiColour == Piece.W) {
            val m = aiPlayer.makeMove(game)
            output.println(m)
        }

        println(game.board)

        while (!game.over()) {
            val oppSan = input.readLine()

            val oppMove =
                game.parseMove(oppSan) ?: error("Invalid move: $oppSan")

            game.applyMove(oppMove)

            println(game.board)

            if (!game.over()) {
                val aiMove = aiPlayer.makeMove(game)

                output.println(aiMove)

                println(game.board)
            }
        }

        println(game.board)

        val winner = game.winner()
        if (winner == null)
            output.println("Stalemate!")
        else
            output.println("Congratulations $winner!")

    }
}

fun main(args: Array<String>) {
    PawnRace().playGame(
        args[0][0],
        PrintWriter(System.out, true),
        BufferedReader(InputStreamReader(System.`in`))
    )
}
