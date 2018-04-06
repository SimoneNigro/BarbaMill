package it.unibo.ai.didattica.mulino.ai;


import java.util.ArrayList;
import it.unibo.ai.didattica.mulino.actions.ByteAction;
import it.unibo.ai.didattica.mulino.domain.BitBoardUtil;
import it.unibo.ai.didattica.mulino.domain.Board;

/**
 * Pseudocode obtained from Aske Plaat's website explaining MTD(f): http://people.csail.mit.edu/plaat/mtdf.html
 *
 * @author dg3213
 */
public class MTDF extends AlphaBeta{
	
    public static final class MoveWrapper<M extends ByteAction> {
        public M action;
        public double score;
        public int evalType;
        public int depth;
    }
    
    //private static final int MaxSearchSize = 250000;
    //private ActionGenerator generator = new ActionGenerator();
    
    public MoveWrapper<ByteAction> Search(Board board, int startingDepth, int maxIterateDepth) {
    	this.stop = false;
    	MoveWrapper<ByteAction> bestMove = null;
        int iterateDepth;
        double firstGuess = 0;
        this.board = board;
        // Call MTD(f) iteratively, allows a more accurate estimate of the true minimax value
        // to be used by the search function at each depth.
        GameTimer++;
        for (iterateDepth = 2; iterateDepth <= maxIterateDepth; iterateDepth++) {
        	NodesSearched = 0; Hits = 0;
        	if(stop)
        		break;
        	
        	//NodesSearched = 0;
        	//System.out.println("Depth "+iterateDepth);
            bestMove = MTD_f(this.board, firstGuess, iterateDepth);
            //System.out.println("Nodi esplorati: "+NodesSearched);
            if (bestMove != null){
                firstGuess = bestMove.score;
                System.out.println("Action: "+bestMove.action+" Depth: "+iterateDepth+"  Score: "+bestMove.score);
                System.out.println("Hit ratio = "+(Hits/NodesSearched)*100);
            }
            if(stop)
        		break;
//            if (NodesSearched > MaxSearchSize) {
//                break;
//            }
        }
        return bestMove;
    }

    private MoveWrapper<ByteAction> MTD_f(Board board, double guess, int depth) {

        double beta;
        double estimate = guess;
        double upperBound = super.MAXVAL;
        double lowerBound = super.MINVAL;

        int maximisingPlayer = 0;
        		//	    		board.currentPlayer;
        MoveWrapper<ByteAction> bestMove;

        do {
            if (estimate == lowerBound)
                beta = estimate + 1;
            else
                beta = estimate;

            bestMove = AlphaBetaWithMemory(this.board, depth, beta - 1, beta, maximisingPlayer, maximisingPlayer);
            if(stop)
            	break;
            if (bestMove != null)
                estimate = bestMove.score;

            if (estimate < beta)
                upperBound = estimate;
            else
                lowerBound = estimate;

        } while (lowerBound < upperBound);

        return bestMove;
    }

    private MoveWrapper<ByteAction> AlphaBetaWithMemory(Board board, int depth, double alpha, double beta, int colour, int maximisingPlayer) {
    	MoveWrapper<ByteAction> bestMove = null;
        ArrayList<ByteAction> possMoves = new ArrayList<>();
        double record = super.MINVAL;
        double score;

        possMoves = (ArrayList<ByteAction>) generator.generateMoves(possMoves, this.board, false);
        //validMoves.generateMoves(board, possMoves, colour);

        for (ByteAction listMove : possMoves) {
            if(stop)
            	break;
            //Board newBoard = board.clone();
            //newBoard = BitBoardUtil.applyAction(newBoard, listMove);
            //newBoard.ApplyMove(listMove);
this.board = BitBoardUtil.applyAction(this.board, listMove);
            score = alphaBeta(this.board, depth - 1, alpha, beta, 1-maximisingPlayer, maximisingPlayer,null);
this.board = BitBoardUtil.revertAction(this.board, listMove);
            alpha = Math.max(alpha, score);

            if (score > record) {
                record = score;
                bestMove = new MoveWrapper<>();
                bestMove.action = listMove;
                bestMove.score = score;

                if (record >= beta) {
                    TransTable.SaveBoard(this.board, record, 2, depth, GameTimer);
                    return bestMove;
                }
                if (record > alpha) {
                    TransTable.SaveBoard(this.board, record, 0, depth, GameTimer);
                }
            }
        }
        return bestMove;
    }
}
