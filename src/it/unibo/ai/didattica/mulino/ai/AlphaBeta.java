package it.unibo.ai.didattica.mulino.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import it.unibo.ai.didattica.mulino.actions.ActionGenerator;
import it.unibo.ai.didattica.mulino.actions.ByteAction;
import it.unibo.ai.didattica.mulino.ai.MTDF.MoveWrapper;
import it.unibo.ai.didattica.mulino.domain.BitBoardUtil;
import it.unibo.ai.didattica.mulino.domain.Board;


public class AlphaBeta {
    int MINVAL = -1000000000;
    int MAXVAL = 1000000000;

    Board board;
    ActionGenerator generator;
    boolean stop;
    TranspositionTable TransTable;
    int GameTimer;
    float NodesSearched;
    float Hits;
    HashMap<Integer,ByteAction[]> killerMoves;

    public AlphaBeta() {
    	stop = false;
        generator = new ActionGenerator();
        TransTable = new TranspositionTable();
        GameTimer = 0;
        NodesSearched = 0;
        Hits = 0;
    	
    }

    // Search to find the best move for the given colour to the given depth:
    public MoveWrapper<ByteAction> Search(Board board, int colour, int startingDepth, int depth) {
    	
    	killerMoves = new HashMap<Integer,ByteAction[]>(50);
    	for(int i = 0; i < 50; i++)
    		killerMoves.put(i, new ByteAction[] {null, null});
    	
    	NodesSearched = 0; Hits = 0;
    	this.stop = false;
        //ArrayList<ByteAction> possMoves = new ArrayList<>();
        this.board = board;
        // First generate the moves for the current player.
        //validMoves.generateMoves(board, possMoves, colour);
       // System.out.println("Dalla board "+this.board);
        ArrayList<ByteAction> possMoves = (ArrayList<ByteAction>) generator.generateMoves(new ArrayList<ByteAction>(32), this.board, false);
        
		Collections.sort(possMoves, moveComparator);

       
        double alpha = MINVAL;
        double beta = MAXVAL;
        double record = MINVAL;
        double score;

        MoveWrapper<ByteAction> bestMove = null;

        // Use the GameTimer to determine how far through the game we are. This allows us to put a time stamp
        // on the entries within the transposition table.
        GameTimer++;

        // Use the iterative deepening method combined with aspiration windows for better move ordering:
        for (int iteration = startingDepth; iteration <= depth; iteration++) {
        	if(stop)
        		break;
        	//partiva da iteration = depth, ed era iter <= depth
            for (ByteAction listMove : possMoves) {
                //Board newBoard = board.clone();
            	if(stop)
            		break;
            	this.board = BitBoardUtil.applyAction(this.board, listMove);
               // newBoard.ApplyMove(listMove);
    

                // Maximise the corresponding value returned
            	//era iteration - 1
                score = alphaBeta(this.board, iteration - 1, alpha, beta, 1-colour, 0, listMove);
                this.board = BitBoardUtil.revertAction(this.board, listMove);
                //System.out.println("Mossa "+listMove+" Score "+score+"Board "+this.board);
                // If score is outside the given window then we must call the next alphaBeta with the
                // original values. Otherwise we may close the window for added efficiency:
                if (score <= alpha || score >= beta) {
                    alpha = MINVAL;
                    beta = MAXVAL;
                } else {
                    alpha = score - 100;
                    beta = score + 100;
                }

                if (score > record) {
                    record = score;
                    bestMove = new MoveWrapper<>();
                    bestMove.action = listMove;
                    bestMove.score = score;
                    //System.out.println("bestMove= "+bestMove.action+ "score= "+bestMove.score+"Iteration= "+iteration);

                }
            }
            if(bestMove != null){
            System.out.println("BestMove a depth "+iteration+": "+bestMove.action);
            System.out.println("Hit ratio = "+(Hits/NodesSearched)*100);
            }
        }
        return bestMove;
    }

    public double alphaBeta(Board board, int depth, double alpha, double beta, int colour, int maximisingPlayer, ByteAction move) {

    	this.board = board;
        MoveWrapper<ByteAction> testMove = new MoveWrapper<ByteAction>();
        NodesSearched++;
        double score;

      //  double originalAlpha = alpha;
       // double originalBeta = beta;
        
        // Check if there is anything suitable within the transposition table first
       if (TransTable.FindBoard(this.board, testMove) && testMove.depth >= depth) {
        	Hits++;
           // System.out.println("Hit ratio = "+(Hits/NodesSearched));

        	// We have found a move for the current board position. Must now check whether it is relevant
            // (i.e. if it has been resolved to a greater depth than we are currently at and what sort of bound
            // has been placed on its evaluation. Note that we can only use certain bounds depending on whether we are
            // maximising or minimising).

            int evalType = testMove.evalType;
           double evaluation = testMove.score;

           if (evalType == 0) {
               return evaluation;
           }
//era 1
           if (evalType == 1) {
               if (evaluation >= alpha) {
                   return alpha;
            	   //alpha = evaluation;
               }
           } else if (evalType == 2) {//era 2
               if (evaluation <= beta) {
            	   return beta;
                   // beta = evaluation;
               }
           }
         //  if(alpha >= beta)
        //	   return evaluation;

        }
        
        if (depth == 0 || isOver() || stop) {

        		score = OurBoardAnalyser.calculateScore(this.board, move);
            	if(colour != 0)
            		score = -score;

                if (score <= alpha) // We have a lower bound
                    TransTable.SaveBoard(board, score, 2, depth, GameTimer);
                else if (score >= beta) // Upper bound
                    TransTable.SaveBoard(board, score, 1, depth, GameTimer);
                else // An exact value: alpha < score < beta
                    TransTable.SaveBoard(board, score, 0, depth, GameTimer);

            return score;
            
        }
        

        ByteAction[] killMoves = killerMoves.get(depth);

        ArrayList<ByteAction> possMoves = new ArrayList<>();
        possMoves = (ArrayList<ByteAction>) generator.generateMoves(possMoves, this.board, false);

		Collections.sort(possMoves, moveComparator);
        
        if(killMoves[0] != null && possMoves.contains(killMoves[0])){
        	//System.out.println("Killer 0: "+killMoves[0]);
        	possMoves.remove(killMoves[0]);
        	possMoves.add(0, killMoves[0]);
        }
        if(killMoves[1] != null && possMoves.contains(killMoves[1])){
        	///System.out.println("Killer 1: "+killMoves[1]);
        	possMoves.remove(killMoves[1]);
        	possMoves.add(0, killMoves[1]);
        }


        if (possMoves.size() == 0) {
            // The current player may have lost all its pieces or none of its pieces may move (i.e. pawns blocked).
            // In this case, the player can be ignored, and will return whatever board is optimal for the next
            // depth.
//        	this.board.currentPlayer = (byte) (1-this.board.currentPlayer);
//        	int temp = OurBoardAnalyser.calculateScoreTommy(this.board);
//        	this.board.currentPlayer = (byte) (1-this.board.currentPlayer);
//            return temp;
            		alphaBeta(this.board, depth - 1, alpha, beta, 1-colour, 0, new ByteAction());
        }

        for (ByteAction listMove : possMoves) {

        	this.board = BitBoardUtil.applyAction(this.board, listMove);
            score = alphaBeta(this.board, depth - 1, alpha, beta, 1-colour, 0, listMove);
        	this.board = BitBoardUtil.revertAction(this.board, listMove);


            if (colour == 0) {
                if (beta <= score) {
                    TransTable.SaveBoard(board, beta, 2, depth, GameTimer);
                    return beta;
                }
                // Otherwise we may have a score between alpha and beta - save this as en exact value.
                if (score >= alpha) {
                    TransTable.SaveBoard(board, score, 0, depth, GameTimer);
                }
                alpha = Math.max(alpha, score);

            } else {
                if (score <= alpha) {
                    TransTable.SaveBoard(board, alpha, 1, depth, GameTimer);
                    return alpha;
                }
                if (score <= beta) {
                    TransTable.SaveBoard(board, score, 0, depth, GameTimer);
                }
                beta = Math.min(beta, score);

            }
        }
        if (colour == 0)
            return alpha;
        else
            return  beta;
        
    

        //return score;
    }
    
    public boolean isOver(){
        return this.hasWon((byte)0) || this.hasWon((byte)1);
    }
    
    public boolean hasWon(byte player){
    return this.board.currentPhase != 1 &&
            (this.board.piecesLeft[1 - player] <= 2 || 
            		this.board.piecesLeft[1 - player] - 
            OurBoardAnalyser.calculateMobility(this.board, 1-player, BitBoardUtil.freeSpaces(this.board.bbs)) == this.board.piecesLeft[1 - player]); // L'avversario non puo' muoversi
    }
    
    public void interrupt(){
    	this.stop = true;
    }
    
    public void resetTT(){
    //	this.TransTable.reset();
    }
    
	private Comparator<ByteAction> moveComparator = new Comparator<ByteAction>() {
		public int compare (ByteAction move1, ByteAction move2) {
			int score1, score2;

			board = BitBoardUtil.applyAction(board, move1);
			score1 = OurBoardAnalyser.calculateScoreTommy(board);
			board = BitBoardUtil.revertAction(board, move1);

			board = BitBoardUtil.applyAction(board, move2);
			score2 = OurBoardAnalyser.calculateScoreTommy(board);
			board = BitBoardUtil.revertAction(board, move2);

			return score1 - score2;
		}
	};
}
    
    /*
    private double QuiescenceSearch(Board board, double alpha, double beta ,int colour, int maximisingPlayer) {
    	MoveWrapper<ByteAction> testMove = new MoveWrapper<ByteAction>();
        NodesSearched++;
        double score;

        // Check if there is anything suitable within the transposition table first.
        if (TransTable.FindBoard(this.board, testMove) && testMove.depth >= 0) {
            int evalType = testMove.evalType;
            double evaluation = testMove.score;

            switch (evalType) {
                case 0:
                    return evaluation;
                case 1:
                    if (evaluation < beta)
                        beta = evaluation;
                    break;
                case 2:
                    if (alpha < evaluation)
                        alpha = evaluation;
                    break;
            }
            if (alpha >= beta)
                return evaluation;
        }

        double record = OurBoardAnalyser.calculateScoreTommy(board);

        if (colour == maximisingPlayer) {
            if(record >= beta)
                return beta;
            if(alpha < record)
                alpha = record;
        }
        else {
            if(record <= alpha)
                return alpha;
            if(record < beta)
                beta = record;
        }

        ArrayList<ByteAction> possMoves = new ArrayList<>();
        possMoves = (ArrayList<ByteAction>) generator.generateMoves(possMoves, this.board, false);
       // validMoves.GenerateMoves(board, possMoves, colour);

        for (ByteAction listMove: possMoves) {
            // Only look at captures from here!
            if (listMove.remove != 99)
                continue;

            //Board_AI newBoard = board.clone();
            //newBoard.ApplyMove(listMove);
            this.board = BitBoardUtil.applyAction(this.board, listMove);
            score = QuiescenceSearch(this.board, alpha, beta, 1-colour, maximisingPlayer);
            this.board = BitBoardUtil.revertAction(this.board, listMove);
            
            if (colour == maximisingPlayer) {
                alpha = Math.max(alpha, score);
                if (beta <= score) {
                    TransTable.SaveBoard(this.board, beta, 1, 0, GameTimer);
                    return beta;
                }
                // Otherwise we have a score between alpha and beta - save this as en exact value.
                if (score > alpha) {
                    TransTable.SaveBoard(this.board, score, 0, 0, GameTimer);
                }
                return alpha;
            }
            else {
                beta = Math.min(beta, score);
                if (score <= alpha) {
                    TransTable.SaveBoard(this.board, alpha, 1, 0, GameTimer);
                    return alpha;
                }
                if (score < beta) {
                    TransTable.SaveBoard(this.board, score, 0, 0, GameTimer);
                }
                return beta;
            }
        }
        if (colour == maximisingPlayer)
            return alpha;
        else
            return beta;
        }
    }*/


