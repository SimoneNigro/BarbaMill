/*
 * Created on Mar 23, 2006
 *
 * (c) 2005 FAO of the UN
 */
package it.unibo.ai.didattica.mulino.ai;

import it.unibo.ai.didattica.mulino.domain.BitBoardUtil;
import it.unibo.ai.didattica.mulino.domain.Board;
import it.unibo.ai.didattica.mulino.actions.Action;
import it.unibo.ai.didattica.mulino.actions.ActionGenerator;
import it.unibo.ai.didattica.mulino.actions.ByteAction;
import it.unibo.ai.didattica.mulino.domain.*;

/**
 * Some observations/assumptions to be used in creating evaluation function:
 * 
 * Max for me, min for her:
 * - Degrees of freedom
 * - Material value
 * - Number of mills
 * 
 * Material values:
 * - Pieces that can form a mill are worth more
 * - Pieces that can form a running mill are worth even more
 *
 * Additional strategies adapted from Paul Emory Sullivan's 'Merrelles' applet:
 *  http://www3.sympatico.ca/pesullivan/merrelles/English.html
 *
 * 
 * Methods ending in "Value" return the evaluation of one side, and take the
 * player to consider as one of the parameters.
 * 
 * Methods ending in "Advantage" calculate the difference between the current
 * player's and the opponents relative score, with higher scores being to the
 * current player's advantage.
 * 
 * Methods ending with "Score" generally implies that some feature of the board 
 * is calculated and weighted 
 * 
 * A row is any three spaces where a mill can be formed. /
 * 
 * We sacrifice some performance for clarity and maintainability of code... 
 * Besides, having clearer code should make optimization easier!
 * 
 * 
 * @author G. Miceli
 */

// Debug messages are commented out with "//". To test board analysis, uncomment
// these lines and the call to logBoardEvaluation in class Game and run the game
// in human vs. human mode. The analysis of each new board will be displayed.
public class BoardAnalyser
{

    static final int[] THREE_WAY_INTERSECTIONS = { 1, 3, 5, 7, 17, 19, 21, 23 };
    static final int THREE_WAY_INTERSECTIONS_BB = BitBoardUtil.positionsToBitBoard(THREE_WAY_INTERSECTIONS);
    static final int[] FOUR_WAY_INTERSECTIONS = { 9, 11, 13, 15 };
    static final int FOUR_WAY_INTERSECTIONS_BB = BitBoardUtil.positionsToBitBoard(FOUR_WAY_INTERSECTIONS);

    static final int WIN_VALUE = 1000000;
    static final int FOUR_WAY_INTERSECTION_VALUE = 64;
    static final int THREE_WAY_INTERSECTION_VALUE = 16;
    static final int SPOKE_EXCLUSIVITY_VALUE = 24;
    static final int MIDDLE_RING_EXCLUSIVITY_VALUE = 16;
    /**
     * Value of having a mill. This is less than ROW_EXCLUSIVITY_VALUE because
     * being able to FORM a mill is more useful than HAVING a mill; pieces are
     * only captured when mills are formed!
     */
    static final int MILL_VALUE = 32;
    /** Value of having only pieces in a row (but not a mill) */
    static final int ROW_EXCLUSIVITY_VALUE = 64;
    /** Value of being able to create a mill */
    static final int POTENTIAL_MILL_VALUE = 160;
    /** Value of being able to create a mill next turn */
    static final int POTENTIAL_MILL_NEXT_TURN_VALUE = 128;

    // Score is multiplied by 2^x where x is the EXPONENT specified here.
    static final int PIECE_ADVANTAGE_EXPONENT = 8;
    static final int MOBILITY_ADVANTAGE_EXPONENT = 6;
    static final int MILL_MOBILITY_EXPONENT = 3;
    static final int SINGLE_JUMPER_EXPONENT = 3;


    public static int calculateScore(Board board)
    {
        int player = board.currentPlayer;
        int score;
        //Board board = state.board;    
        int otherPlayer = 1 - player;
        int freebb = BitBoardUtil.freeSpaces(board.bbs);
        		//state.features.freebb;

        // Lose/win by capture.  
        if (board.piecesLeft[player] < 3)
            return -BoardAnalyser.WIN_VALUE;
        else if (board.piecesLeft[otherPlayer] < 3) 
            return BoardAnalyser.WIN_VALUE;      
        
        // Piece advantage
        score = BoardAnalyser.calculatePieceAdvantage(board, player)  << BoardAnalyser.PIECE_ADVANTAGE_EXPONENT;
        
        int jumping = (board.piecesLeft[0] == 3 ? 1 : 0) + 
                      (board.piecesLeft[1] == 3 ? 1 : 0);
       
//        boardHashCode = BoardAnalyser.calculateHashCode(state.board.bbs);
        
        // Decide based on # of player that can "jump" (i.e. in endgame)
        switch ((board.piecesLeft[0] == 3 ? 1 : 0) + 
                (board.piecesLeft[1] == 3 ? 1 : 0))
        {
        // in opening and midgame:
        case 0:
            int playerMobility = BoardAnalyser.calculateMobility(board, player);
            int opponentMobility = BoardAnalyser.calculateMobility(board, 1-player);
            int mobilityAdvantageScore = (playerMobility-opponentMobility) << BoardAnalyser.MOBILITY_ADVANTAGE_EXPONENT;

            if (playerMobility == 0 && board.unplacedPieces[player] == 0)
                return -BoardAnalyser.WIN_VALUE;
            else if (opponentMobility == 0 && board.unplacedPieces[otherPlayer] == 0) 
                return BoardAnalyser.WIN_VALUE;

            // Advantage for having more degrees of freedom of motion (a.k.a.
            // mobility)
            // death by "trapping" (no mobility) can only happen in opening and
            // midgame.
            score += mobilityAdvantageScore;
            // Extra points for having pieces on a T or X
            score += BoardAnalyser.calculateIntersectionAdvantageScore(board, player);
            score += BoardAnalyser.calculateRowAdvantage(board, player, freebb);

            break;
        // if in endgame and only one side has 3 pieces left::
        case 1:
            score <<= BoardAnalyser.SINGLE_JUMPER_EXPONENT;
            // Extra points for having pieces on a T or X
            score += BoardAnalyser.calculateIntersectionAdvantageScore(board, player);
            score += BoardAnalyser.calculateRowAdvantage(board, player, freebb);
        // if one or both players can "jump" (i.e. have 3 pieces left)
        // then regulate AGGRESSION of computer player
        default:
            // TODO Calculate defense/attack posture and regulate aggression accordingly.  For rely on search
        }

        return score;
    }

    public static int calculatePieceAdvantage(Board board, int player)
    {
        return board.piecesLeft[player] - board.piecesLeft[1-player];
    }

    public static int calculateIntersectionAdvantageScore(Board board, int player)
    {
        return Integer.bitCount(board.bbs[player] & THREE_WAY_INTERSECTIONS_BB) * THREE_WAY_INTERSECTION_VALUE 
             + Integer.bitCount(board.bbs[player] & FOUR_WAY_INTERSECTIONS_BB) * FOUR_WAY_INTERSECTION_VALUE;
    }

    public static int calculateRowAdvantage(Board board, int player, int freebb)
    {
        int score = 0;
        int mybb = board.bbs[player];
        int herbb = board.bbs[1 - player];
        int myrow, herrow;

        for (int millno = 0; millno <= 15; millno++)
        {
            int millmask = BoardFeatures.MILL_POSITION_BBS[millno];
            myrow = mybb & millmask;
            herrow = herbb & millmask;

            // If current player is only player in row, bonus
            if (myrow > 0 && herrow == 0)
            {
                score += calculateRowValue(board, player, player, freebb, millno, millmask, mybb, myrow);
            }
            // If other player is only player in row, penalty
            else if (herrow > 0 && myrow == 0)
            {
                score -= calculateRowValue(board, 1-player, player, freebb, millno, millmask, herbb, herrow);
            }
        }
        return score;
    }

    /**
     * Not called during endgame when pieces can jump. Player must also be only
     * player in row
     * 
     */
    private static int calculateRowValue(Board board, int player, int currentPlayer, 
                                         int freebb, int millno, int millmask, int bb, int rowbb)
    {
        int rowfreebb = millmask & ~rowbb; // free spaces in selected row/mill

        int score = 0;

        // Bonus for trying to make a mill along horizontals or verticals
        if (millno >= BoardFeatures.SPOKES_IDX)
        {
            score += SPOKE_EXCLUSIVITY_VALUE;
        }
        // Bonus for trying to make a mill along middle ring
        else if (millno >= BoardFeatures.MIDDLE_RING_IDX && millno < BoardFeatures.INNER_RING_IDX)
        {
            score += MIDDLE_RING_EXCLUSIVITY_VALUE;
        }

        switch (Integer.bitCount(rowbb))
        {
        // if this is a mill, give a score based on how easy it is
        // to cycle (open/close)
        case 3:
            score += MILL_VALUE;
            score += calculateMobility(freebb, rowbb) << MILL_MOBILITY_EXPONENT;
            break;
        case 2:
            // if about to create a mill (2 pieces in row), give more points

            // if the specified player still has pieces to place
            // or has piece nearby that can slide into this row
            if (board.unplacedPieces[player] > 0    
                || canSlideIntoRow(millmask, bb, rowfreebb, rowbb, player)) 
            {
                if (player == currentPlayer)
                {
                    score += POTENTIAL_MILL_VALUE;
                }
                else
                {
                    score += POTENTIAL_MILL_NEXT_TURN_VALUE;
                }
            }
            
        case 1:
            // give a bonus for being only one in row (for both 1 or 2 pieces)
            score += ROW_EXCLUSIVITY_VALUE;
            break;
        }

        return score;
    }

    public static boolean canSlideIntoRow(int millmask, int bb, int rowfreebb, int rowbb, int player)
    {
        int notinrowbb = ~rowbb;
        // for each empty/free position in the current row,
        for (int pos = 0; rowfreebb > 0; rowfreebb >>>= 1, pos++)
            if ((rowfreebb & 1) != 0)
            {
                // get all adjacent positions not in the current row
                int adjbb = ActionGenerator.ADJACENT_POSITION_BBS[pos] & notinrowbb;

                // if the current player has one more pieces in these positions,
                // player
                // can create a mill by sliding one of those pieces into the
                // current row
                if ((bb & adjbb) != 0) return true;

            }
        // not adjacent pieces can slide into any empty slots in row - fail!
        return false;
    }
    
    public static int calculateMobility(Board board, int player)
    {
        return calculateMobility(BitBoardUtil.freeSpaces(board.bbs), board.bbs[player]);
    }
    
    private static int calculateMobility(int freebb, int bb)
    {
        int m = 0;
        int adjbb;

        // for each space in BB
        for (int pos = 0; bb != 0; bb >>>= 1, pos++)
            // which contains a piece
            if ((bb & 1) != 0)
            {
                // count pieces can move to any space that is adjacent and free
                m += Integer.bitCount(ActionGenerator.ADJACENT_POSITION_BBS[pos] & freebb);
            }

        return m;
    }


}


