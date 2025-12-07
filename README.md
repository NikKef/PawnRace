# The Game

## Description

Gerry and Bobby meet on a plane. They discover that they both like chess, and that one of
them is carrying a portable chess board. But alas! Gerry has to sneeze as he opens the board,
and a few pieces fall out! After a short but frustrating search, they give up and decide to delay
further searching efforts until the plane has landed. Taking a look at the remaining pieces,
Bobby has an idea: He challenges Gerry to a pawn race. The rules would be simple – the game
consists only of pawns, which are all set in their usual starting positions; the player who first
promotes one of his pawns to the last rank wins. After briefly thinking about it, Gerry decides
to accept the challenge, but with one slight modification: Each player would only play with
seven pawns, thus leaving a gap somewhere in the line of pawns. Since white has the advantage
of starting the game, Gerry thought it would only be fair if the black player chooses where the
gaps are.

## How to play

### Board and setup

Pawn races are played on a normal chess board, with 8x8 squares. Rows are commonly referred
to as ranks, and are labelled 1-8, while columns are referred to as files, labelled A-H. From
white’s perspective, the square in the bottom left corner would thus be referred to as a1, while
the bottom right corner is h1. White’s pawns are all placed on the second rank initially, while
black starts from the seventh rank.

### Pawns and pawn moves

Pawns are considered the simplest pieces on the chess board, yet they often build the back-bone
for even very advanced and complex strategies employed by grandmasters. This is despite the
fact that unlike other chess pieces, pawns cannot make particularly complex moves, can only
move in very limited ways, and only in the forward direction. To illustrate how pawns can move
around the chess board. The following rules apply:

* A pawn can move straight forward by 1 square, if the targeted square is empty.
* A pawn can move straight forward by 2 squares, if it is on its starting position, and both
the targeted square and the passed-through square are empty.
* A pawn can move diagonally forward by 1 square, iff that square is occupied by an
opposite-coloured pawn. This constitutes a capture, and the captured pawn is taken off
the board.
* Combining the previous two rules, if a pawn has moved forward by 2 squares in the last
move played, it may be captured on the square that it passed through. This special type
of capture is a capture in passing and commonly referred to as the En Passant rule. A
pawn can only be captured en passant immediately after it moved forward two squares,
but not at any later stage in the game.

### Algebraic chess notation

There are many ways of denoting moves in a chess game. The most popular one, however, is
certainly the standard algebraic notation, which is also used by FIDE, the Federation Internationale
des Echecs or World Chess Federation, across all competetive matches. Its variant, the
long algebraic notation, records both start and target square of any move (sperated by a dash).
However, in standard algebraic notation the starting square is omitted, if there is no ambiguity.

### Gameplay

Traditionally, the white player always starts the game. Both players take turns to make moves.
If a player cannot make any valid move because all his pawns are blocked from moving, the
game is considered a stale-mate, which is a draw. Whichever player first manages to promote
one of his pawns all the way to the last rank, as seen from his own perspective, wins the game.
However, the game can also be won by a player capturing all of the opponent’s pawns.

## Playing with the AI

You can launch the game and the AI player straight from the main function of the program using the
provided compiled jar file <code>pawnrace.jar</code>. Run this command replacing **colour** with
the colour you want the AI to play as.

<code>java -jar pawnrace.jar ***colour***</code>

After that, the player playing with the Black Pieces will need to give the file gaps (two 
characters from A-H) with the white gap first. For example,

<code>AH</code>

After that, you need to repeat the above gaps (even if you are the white player) so that
the bot registers them correctly.

After that you can play the game using the standard short algebraic notation.

Have fun!

-----------------------------
© 2025 Nikolas Kefalonitis
