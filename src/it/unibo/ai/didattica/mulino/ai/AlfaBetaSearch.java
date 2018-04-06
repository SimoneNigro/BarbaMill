package it.unibo.ai.didattica.mulino.ai;

import java.security.acl.LastOwnerException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import it.unibo.ai.didattica.mulino.actions.Action;
import it.unibo.ai.didattica.mulino.actions.ActionGenerator;
import it.unibo.ai.didattica.mulino.actions.ByteAction;
import it.unibo.ai.didattica.mulino.domain.BitBoardUtil;
import it.unibo.ai.didattica.mulino.domain.Board;

public class AlfaBetaSearch<M extends ByteAction> {

    static final class MoveWrapper<M extends ByteAction> {
        public M action;
    }
    
	Board board;
	ActionGenerator actionGenerator;
	boolean stop;
	
	public AlfaBetaSearch(){
		this.actionGenerator = new ActionGenerator();
		this.stop = false;
	}
	
    public void setBoard(Board board){
    	this.board = board;
    }
	
    private final long alphabeta(final MoveWrapper<M> wrapper, final int depth, final int who, long alpha, long beta, M lastMove) {
        if (depth == 0 || stop|| isOver()) {
            //this.stop = false;
       // long evaluate = evaluate();    
        //System.out.println("Evaluate: "+evaluate);
          //  System.out.println("chosenAction da valutare: "+chosenAction);
            return who * evaluate(lastMove);
        }
        M bestMove = null;
        long score;
        Collection<M> moves = (Collection<M>) generateSuccessors();
        //System.out.println("moves: "+moves);
        if (moves.isEmpty()){
        	this.board.currentPlayer = (byte) (1-this.board.currentPlayer);
            score = alphabetaScore(depth, who, alpha, beta, (M)(new ByteAction()));
        	this.board.currentPlayer = (byte) (1-this.board.currentPlayer);
            return score;
        }
        if (who > 0) {
            for (M move : moves) {
            	if(stop)
            		break;
                this.board = BitBoardUtil.applyAction(this.board, move);
                score = alphabetaScore(depth, who, alpha, beta, move);
                //System.out.println("Score: "+score);
                this.board = BitBoardUtil.revertAction(this.board, move);
              //  System.out.println("Score: "+score+" Alfa: "+alpha);
                if (score > alpha) {
                    alpha = score;
                    //System.out.println("Score > Alpha");
                    bestMove = move;
                    if (alpha >= beta) {
                        break;
                    }
                }
            }

            if (wrapper != null) {
                wrapper.action = bestMove;
            }//            System.out.println("bestMove "+bestMove);
    //        System.out.println("chosenAction "+chosenAction);
            //this.stop = false;

            return alpha;
            
        } else {
            for (M move : moves) {
            	if(stop)
            		break;
                this.board = BitBoardUtil.applyAction(this.board, move);
                score = alphabetaScore(depth, who, alpha, beta, move);
               this.board = BitBoardUtil.revertAction(this.board, move);
                if (score < beta) {
                    beta = score;
                    bestMove = move;
                    if (alpha >= beta) {
                        break;
                    }
                }
            }
        //    System.out.println("bestMove: "+bestMove);
           // System.out.println("chosenAction: "+bestMove+" chosenAction.putPos "+((FirstAction)chosenAction).putPosition);
            
            if (wrapper != null) {
                wrapper.action = bestMove;
            }//            System.out.println("bestMove "+bestMove);
       //     System.out.println("chosenAction "+chosenAction);
            //this.stop = false;

            return beta;
        }
    }

    protected long alphabetaScore(final int depth, final int who, final long alpha, final long beta, final M lastMove) {
		return alphabeta(null, depth - 1, -who, alpha, beta, lastMove);
	}
	
    protected int evaluate(ByteAction lastAction)
    {         
    	if (this.hasWon(this.board.currentPlayer)) { // Se vinco e' la mossa migliore
            return +1000000000;
        } else if (this.hasWon((byte) (1-this.board.currentPlayer))) { // Se perdo e' la mossa peggiore
            return -1000000000;
        }
    	//System.out.println("Valuta lei: "+this.board);
     //   return OurBoardAnalyser.calculateScore(this.board, lastAction);
    	 return OurBoardAnalyser.calculateScoreTommy(this.board);
    }
    protected List<ByteAction> generateSuccessors()
    {
    	return actionGenerator.generateMoves(new ArrayList<ByteAction>(3 * (24 - this.board.piecesLeft[0] -this.board.piecesLeft[1])),
    			this.board, false);
    } 
    public ByteAction getBestAction(final int depth) {

    	this.stop = false;
        MoveWrapper<M> wrapper = new MoveWrapper<>();

        long score = alphabeta(wrapper, depth, 1, -1000000000, 1000000000, (M)(new ByteAction()));
        //System.out.println("Res = "+wrapper.action);
        if (depth <= 0) {
            throw new IllegalArgumentException("Search depth MUST be > 0");
        }
      // System.out.println("Score a depth "+depth+"= "+score);
        //System.out.println("Della board "+this.board+"\n\n");
        return wrapper.action;
    }
    public void interrupt(){
    	this.stop = true;
    }
    
    public boolean isOver(){
        return this.hasWon((byte)0) || this.hasWon((byte)1);
    }
    
    public boolean hasWon(byte player){
    return this.board.currentPhase != 1 &&
            (this.board.piecesLeft[1 - player] <= 2 || 
            		this.board.piecesLeft[1 - player] - 
            OurBoardAnalyser.calculateMobility(board, 1-player, BitBoardUtil.freeSpaces(this.board.bbs)) == this.board.piecesLeft[1 - player]); // L'avversario non puo' muoversi
    }
}
