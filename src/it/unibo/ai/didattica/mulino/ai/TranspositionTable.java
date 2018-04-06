package it.unibo.ai.didattica.mulino.ai;

import java.util.HashMap;

import it.unibo.ai.didattica.mulino.actions.ByteAction;
import it.unibo.ai.didattica.mulino.ai.MTDF.MoveWrapper;
import it.unibo.ai.didattica.mulino.domain.Board;

/**
 * Created by dg3213 on 28/03/14.
 */

class TableEntry {
    //private long hash;
    private int depth;
    private int flag;
    private double eval;
    private int time;
    private byte currentPlayer;

    public TableEntry() {
        this.flag = -1;
    }

    public TableEntry( int depth, int flag, double eval, int timeStamp, byte currentPlayer) {
        //this.hash = hash;
        this.depth = depth;
        this.flag = flag;
        this.eval = eval;
        this.time = timeStamp;
        this.currentPlayer = currentPlayer;
    }
/*
    public long getHash() {
        return hash;
    }*/

    public int getFlag() {
        return flag;
    }

    public double getEval() {
        return eval;
    }

    public int getTime() {
        return time;
    }

    public int getDepth() {
        return depth;
    }

    public byte getCurrentPlayer() {
        return currentPlayer;
    }
}

public class TranspositionTable {
    // Make sure hash table size is prime
    /*private static final int HASH_SIZE = 131303;
    private TableEntry TransTable[];

    // Construction
    public TranspositionTable() {
        TransTable = new TableEntry[HASH_SIZE];
        for (int i = 0; i < HASH_SIZE; i++) {
            TransTable[i] = new TableEntry();
        }
    }*/
	
	HashMap<Long,TableEntry> TransTable = new HashMap<Long,TableEntry>((int) Math.pow(2, 22));

    // Check to see if there is already a stored board position within the transposition table.
    // If so then we also have a best move for that position (either an upper bound/lower bound/exact value)
    // so copy the values in the table to the input parameter 'move'.
    public boolean FindBoard(Board board, MoveWrapper<ByteAction> move) {
        long key = board.getTransposition();
        // Find the board's hash position in Table
        TableEntry entry = TransTable.get(key);

        // Check flag - empty entries are set to 1. If so then then there is not yet an entry corresponding to the board position.
        if (entry == null)
            return false;

        // Also make sure that the actual board zobrist keys match - not just the hash table entries!
        //if (entry.getZobrist() != zobristKey)
        //   return false;

        // If we find a match then copy into the given move parameter
       // if(board.currentPlayer == entry.getCurrentPlayer())
        	move.score = (entry.getEval());
        //else
        //	move.score = - (entry.getEval());
        
        move.evalType = (entry.getFlag());
        move.depth = (entry.getDepth());
        return true;
    }

    // Save the board using Zobrist's key as identity.
    public boolean SaveBoard(Board board, double evaluation, int flag, int depth, int timeStamp) {
        long key = board.getTransposition();

        // If there already exists a better move in the transposition table (i.e. of greater depth)
        // then don't erase it!
        if(TransTable.get(key) != null){
	        if ((TransTable.get(key).getFlag() > 0) && (TransTable.get(key).getDepth() > depth) && (TransTable.get(key).getTime() >= timeStamp))
	            return true;
        }
        // If no better move found then add an entry:
        TransTable.put(key,new TableEntry(depth, flag, evaluation, timeStamp, board.currentPlayer));

        return true;
    }
    
    public void reset(){
    	this.TransTable.clear();
    }
}
