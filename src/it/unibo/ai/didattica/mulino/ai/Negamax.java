package it.unibo.ai.didattica.mulino.ai;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import it.unibo.ai.didattica.mulino.actions.ActionGenerator;
import it.unibo.ai.didattica.mulino.actions.ByteAction;
import it.unibo.ai.didattica.mulino.domain.BitBoardUtil;
import it.unibo.ai.didattica.mulino.domain.Board;

/**
 * A chess searcher that uses:
 * 
 * Negamax
 * Iterative Deepening 
 * Transposition Table
 * Quiescence Search
 * Move ordering (captures and promotions first)
 * Killer heuristic (best two moves seen are stored and tried first)
 * Avoid repetitions (to avoid draws)
 * Check extension (searches one ply deeper when board is in check)
 * 
 * TODO
 * Better evaluation function
 * 
 * Resources:
 * http://homepages.cwi.nl/~paulk/theses/Carolus.pdf
 * http://www.cs.unm.edu/~aaron/downloads/qian_search.pdf
 * http://www.frayn.net/beowulf/theory.html
 * http://people.csail.mit.edu/plaat/mtdf.html
 * http://www.tzi.de/~edelkamp/lectures/ki/slides/two-slides.pdf
 * http://www.top-5000.nl/ps/SomeAspectsOfChessProgramming.pdf
 * http://facta.junis.ni.ac.rs/acar/acar200901/acar2009-07.pdf
 */

public class Negamax extends AbstractSearcher {

	// How deep our quiescence search goes
	private final int QUIESCENCE_MIN_DEPTH = 2;
	private final int QUIESCENCE_MID_DEPTH = 5;
	private final int QUIESCENCE_MAX_DEPTH = 100;

	// Create hash that contains previously processed nodes.
	private final int TRANSPOSITION_INIT_ELEMS = 2 << 20;
	private final int TRANSPOSITION_MAX_ELMS = TRANSPOSITION_INIT_ELEMS;
	private final float TRANSPOSITION_LOAD_FACTOR = 0.75f;
	private Map<Board, BoardInfo> transpositionTable;

	// Store the information about repetitions
	//private BoardCount boardCount = new BoardCount();

	// The depth the iterative deepening is currently at
	private int depthIteration = 0;

	Board board;

	// Variables for performance analysis
	private ThreadMXBean bean;
	private long nodeCount, hits;
	private int originalMinDepth = -1;
	private boolean stop;

	public void interrupt(){
		this.stop = true;
	}
	
	public ByteAction getBestMove(Board board, int startingDepth, int maxDepth) {
		this.stop = false;
		nodeCount = 0; hits = 0;
		
		this.board = board;		
		transpositionTable = new LRUMap<Board, BoardInfo>(TRANSPOSITION_INIT_ELEMS,
				TRANSPOSITION_MAX_ELMS,
				TRANSPOSITION_LOAD_FACTOR);				

		System.out.println("Board prima di generare le mosse "+this.board);
		LinkedList<ByteAction> moves = generateOrderedMoves();
		System.out.println("Board dopo aver generato le mosse "+this.board);
		System.out.println("Moves: "+moves);
		Collections.sort(moves, moveComparator);
		System.out.println("Board dopo aver ordinato le mosse "+this.board);
		System.out.println("Ordered Moves: "+moves);

		// Should never be called if we don't have any moves left
		if (moves.isEmpty()) {
			throw new IllegalStateException(); 
		}

		int infinity = 1000000000;
		ByteAction bestMove = moves.getFirst();

		// To do Iterative deepening, we have to repeat our alpha-beta search
		// from n = 1 ... depth. We keep track of the move ordering
		// so each future depth starts with the best possible moves	
		for(depthIteration = startingDepth; depthIteration <= maxDepth; depthIteration++) {

			// We've hit the minimum depth, so timeup regularly
//			if(depthIteration >= minDepth) {
//				timer.okToTimeup();
//			}

			if(stop) {
				break;
			}
			// We may return from this call before we searched full depth, so verify result					    
			ByteAction unverifiedBestMove = rootNegaMax(moves, depthIteration, -infinity, infinity);
			if (unverifiedBestMove != null) {
				bestMove = unverifiedBestMove;
				//reportNewBestMove(bestMove);

				// Add the best move to the start of the list
				moves.remove(bestMove);
				moves.addFirst(bestMove);
				System.out.println("Depth: "+depthIteration+" % Hits: "+((float)hits*100)/nodeCount+" BestMove: "+bestMove);
			}
			if(stop) {
				break;
			}
		}	
		
		// Track the result so we don't repeat it
//		board.applyMove(bestMove);
//		boardCount.increment(board);
//		board.undoMove();
		return bestMove;
	}

	// negamax() at the root level, allowing us to keep track of the best move
	private ByteAction rootNegaMax(LinkedList<ByteAction> moves, int depth, int alpha, int beta) {
		nodeCount++;

		// Extend search when we find a player in check
//		if(board.inCheck()) {
//			depth++;
//		}
		BoardInfo boardInfo = transpositionTable.get(this.board);

		// See if we have a cache hit
		if((boardInfo != null) && (boardInfo.getDepth() >= depth)) {
			hits++;

			// We can just return, because our guess is as good or better in the hash
			if(boardInfo.getType() == BoardInfo.EXACT) {
				return boardInfo.getBestMove();
			}

			// Hash has a better lower bound, so set our current lower bound
			else if(boardInfo.getType() == BoardInfo.LOWER &&
					boardInfo.getValue() > alpha) {
				alpha = boardInfo.getValue();
			}

			// Hash has a better upper bound, so set our current upper bound
			else if(boardInfo.getType() == BoardInfo.UPPER &&
					boardInfo.getValue() < beta) {
				beta = boardInfo.getValue();
			}

			// If lower bound surpasses upper bound, return value (alpha/beta pruning)
			if(alpha >= beta) {
				return boardInfo.getBestMove();
			}
		}

		// Note - we don't have to order the moves here, since they are ordered
		// by our iterative deepening and passed as an argument here, in order

		// Best thing we've seen so far is -infinity
		int bestValue = -1000000000;
		ByteAction bestMove = moves.getFirst();
		int value;

		for (ByteAction move : moves) {

			// If we're out of time, get out of the loop and return failed value
			if(stop) {
				return null;
			}

			// Compute the new best Value
			this.board = BitBoardUtil.applyAction(this.board, move);
			value = -negamax(depth-1, -beta, -alpha);
			this.board = BitBoardUtil.revertAction(this.board, move);

			// We found a new max, also keep track of move
			if(value > bestValue) {
				bestValue = value;
				bestMove = move;
			}

			// If our max is greater than our lower bound, update our lower bound
			if(bestValue > alpha) {
				alpha = bestValue;
			}

			// Alpha-beta pruning
			if(bestValue >= beta) {
				break;
			}
		}

		updateTranspositionTable(alpha, beta, depth, bestValue, bestMove);
		return bestMove;
	}

	// Negamax with transposition tables and move ordering
	private int negamax(int depth, int alpha, int beta) {
		nodeCount++;

		// Extend search when we find a player in check
//		if(board.inCheck()) {
//			depth++;
//		}

		// See if we have a cache hit
		BoardInfo boardInfo = transpositionTable.get(this.board);

		if((boardInfo != null) && (boardInfo.getDepth() >= depth)) {

			hits++;
			
			// We can just return, because our guess is as good or better in the hash
			if(boardInfo.getType() == BoardInfo.EXACT) {
				return boardInfo.getValue();
			}

			// Hash has a better lower bound, so set our current lower bound
			else if(boardInfo.getType() == BoardInfo.LOWER &&
					boardInfo.getValue() > alpha) {
				alpha = boardInfo.getValue();
			}

			// Hash has a better upper bound, so set our current upper bound
			else if(boardInfo.getType() == BoardInfo.UPPER &&
					boardInfo.getValue() < beta) {
				beta = boardInfo.getValue();
			}

			// If lower bound surpasses upper bound, return value (alpha/beta pruning)
			if(alpha >= beta) {
				return boardInfo.getValue();
			}
		}

		// Once we've seen a repetition already, consider that board a stalemate		
//		if (boardCount.isRepetition(board)) {
//			return -evaluator.stalemate();
//		}

		// Base case
		if (depth == 0) {

			// Search more until we hit a quiet position. Limit search on this
			// so we don't time out on a super long leaf chain		
			if (depthIteration < minDepth) {
				return quiescenceSearch(QUIESCENCE_MIN_DEPTH, alpha, beta);				
			}
			else if (depthIteration == minDepth) {
				return quiescenceSearch(QUIESCENCE_MID_DEPTH, alpha, beta);							
			}
			return quiescenceSearch(QUIESCENCE_MAX_DEPTH, alpha, beta);
			//return OurBoardAnalyser.calculateScoreTommy(this.board);
		}

		// Get the moves we can make
		LinkedList<ByteAction> moves = generateOrderedMoves();

		// No moves to make
		if (moves.isEmpty()) {
			return -10000000;//-negamax(depth-1, -beta, -alpha);
		}			
		else {

			// Add killer moves to front of list
			orderMoves(moves, boardInfo);

			// Best thing we've seen so far is -infinity
			int bestValue = -1000000000;
			int value;

			// We know it's not empty so get the first move to start
			ByteAction bestMove = moves.getFirst();
			value = -1000000000;

			for (ByteAction move : moves) {

				// If we're out of time, get out of the loop and ignore these results
				if(stop) {
					return -1000000000;
				}

				// Compute the new best Value
				this.board = BitBoardUtil.applyAction(this.board, move);
				value = -negamax(depth-1, -beta, -alpha);
				this.board = BitBoardUtil.revertAction(this.board, move);

				// We found a new max, also keep track of move
				if(value > bestValue) {
					bestValue = value;
					bestMove = move;
				}

				// If our max is greater than our lower bound, update our lower bound
				if(bestValue > alpha) {
					alpha = bestValue;
				}

				// Alpha-beta pruning
				if(bestValue >= beta) {
					break;
				}				
			}

			updateTranspositionTable(alpha, beta, depth, bestValue, bestMove);

			return bestValue;
		}
	}

	// Quiescence Search With Transposition Table - Searches to make sure we aren't giving an
	// artificial advantage to a move because of the limit in our depth tree
	private int quiescenceSearch(int depth, int alpha, int beta) {
		nodeCount++;

		// We've already gone through depthIteration iterations of negamax
		// so our real depth is depthIteration + the depth value in this call
		int quiescenceDepth = depth + depthIteration;

		// See if we have a cache hit
		BoardInfo boardInfo = transpositionTable.get(this.board);

		if((boardInfo != null) && (boardInfo.getDepth() >= quiescenceDepth)) {

			// We can just return, because our guess is as good or better in the hash
			if(boardInfo.getType() == BoardInfo.EXACT) {
				return boardInfo.getValue();
			}

			// Hash has a better lower bound, so set our current lower bound
			else if(boardInfo.getType() == BoardInfo.LOWER &&
					boardInfo.getValue() > alpha) {
				alpha = boardInfo.getValue();
			}

			// Hash has a better upper bound, so set our current upper bound
			else if(boardInfo.getType() == BoardInfo.UPPER &&
					boardInfo.getValue() < beta) {
				beta = boardInfo.getValue();
			}

			// If lower bound surpasses upper bound, return value (alpha/beta pruning)
			if(alpha >= beta) {
				return boardInfo.getValue();
			}
		}

		int value = OurBoardAnalyser.calculateScoreTommy(this.board);		

		// Base case
		if(depth == 0) {
			return value;
		}

		// Standing pat causes a beta cutoff
		if(value >= beta) {
			return value;
		}

		LinkedList<ByteAction> moves = generateNonQuietMoves();					

		// We are in a "quiet" position, so finish
		if(moves.isEmpty()) {
			return value;
		}

		// Standing pat score can become new alpha
		if(value > alpha) {
			alpha = value;
		}		

		// Ordering is important for quiescence search since we're comparing captures
		// so we need to distinguish the best moves. Empirically, sorting here is faster
		Collections.sort(moves, moveComparator);
		orderMoves(moves, boardInfo);

		// Our best value up to this point is our lower bound
		int bestValue = -1000000000;

		// We know it's not empty...so just get any move to start
		ByteAction bestMove = moves.getFirst();
		for (ByteAction move : moves) {

			// If we're out of time, get out of the loop and ignore these results
			if(stop) {
				return -1000000000;
			}

			// Compute the new best value
			this.board = BitBoardUtil.applyAction(this.board, move);
			value = -quiescenceSearch(depth-1, -beta, -alpha);
			this.board = BitBoardUtil.revertAction(this.board, move);

			// We found a new max, also keep track of move
			if(value > bestValue) {
				bestValue = value;
				bestMove = move;
			}

			// If our max is greater than our lower bound,
			// update our lower bound
			if(bestValue > alpha) {
				alpha = bestValue;
			}

			// Alpha-beta pruning
			if(bestValue >= beta) {
				break;
			}
		}

		updateTranspositionTable(alpha, beta, quiescenceDepth, bestValue, bestMove);
		return bestValue;
	}

	// Store results in the transposition table as a lower bound, upper bound, or exact value	
	private void updateTranspositionTable(int alpha, int beta, int depth, int bestValue, ByteAction bestMove) {
		if(bestValue <= alpha) {
			updateTranspositionTable(bestValue, bestMove, BoardInfo.LOWER, depth);
		}
		else if(bestValue >= beta) {
			updateTranspositionTable(bestValue, bestMove, BoardInfo.UPPER, depth);				
		}
		else {
			updateTranspositionTable(bestValue, bestMove, BoardInfo.EXACT, depth);
		}				
	}

	// Either add or update entry in transposition table
	private void updateTranspositionTable(int value, ByteAction move, int type, int depth) {
		BoardInfo boardInfo = transpositionTable.get(this.board);
		if(boardInfo != null) {
			boardInfo.updateInfo(value, move, type, depth);
		}
		else {
			transpositionTable.put(this.board, new BoardInfo(value, move, type, depth));
		}
	}

	// Adds the killer moves to the top of the list
	private void orderMoves(LinkedList<ByteAction> moves, BoardInfo boardInfo) {
		if(boardInfo != null) {
			if(boardInfo.getSecondBestMove() != null) {
				moves.remove(boardInfo.getSecondBestMove());
				moves.addFirst(boardInfo.getSecondBestMove());					
			}
			moves.remove(boardInfo.getBestMove());
			moves.addFirst(boardInfo.getBestMove());				
		}		
	}

	// Order the moves (captures/promotions first)
	// Doing a full sort in this method was not cost effective
	private LinkedList<ByteAction> generateOrderedMoves() {
		List<ByteAction> psmoves = new ActionGenerator().generateMoves(new ArrayList<ByteAction>(32), this.board, false);
		LinkedList<ByteAction> moves = new LinkedList<ByteAction>();
		Set<ByteAction> setmoves = new HashSet<ByteAction>(256);

		for(ByteAction m : psmoves) {
			if(setmoves.add(m)) {			
				if(m.remove != 99) {
					moves.addFirst(m);	
				}
				else {
					moves.add(m);
				}
			}
		}
		return moves;
	}

	// Generates a list of moves that are only captures and promotions
	public LinkedList<ByteAction> generateNonQuietMoves() {
		ActionGenerator generator = new ActionGenerator();
		List<ByteAction> psmoves = generator.generateMoves(new ArrayList<ByteAction>(32), this.board, false);
		LinkedList<ByteAction> moves = new LinkedList<ByteAction>();
		Set<ByteAction> setmoves = new HashSet<ByteAction>(256);

		// Check all plausible moves
		for(ByteAction m : psmoves) {

			// Has to be legal and unique
			if(setmoves.add(m)) {

				// Only place captures and promotions in this list
				if(m.remove != 99) {
					moves.add(m);	
				}
			}
		}
		return moves;
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