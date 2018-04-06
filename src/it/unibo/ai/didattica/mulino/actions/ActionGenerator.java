package it.unibo.ai.didattica.mulino.actions;

import java.util.ArrayList;
import java.util.List;

import it.unibo.ai.didattica.mulino.domain.BitBoardUtil;
import it.unibo.ai.didattica.mulino.domain.Board;
import it.unibo.ai.didattica.mulino.domain.BoardFeatures;


public class ActionGenerator
{
    public static final int[][] ADJACENT_POSITIONS =
    {
        // Outer ring (clockwise from top-left)
        {7, 1},             // 0
        {0, 2, 9},          // 1
        {1, 3},             // 2
        {2, 4, 11},         // 3
        {3, 5},             // 4
        {4, 6, 13},         // 5
        {5, 7},             // 6
        {6, 0, 15},         // 7
        
        // Middle ring (clockwise from top-left)
        {15,  9},           // 8
        { 8, 10, 1, 17},    // 9
        { 9, 11},           // 10
        {10, 12, 3, 19},    // 11
        {11, 13},           // 12
        {12, 14, 5, 21},    // 13
        {13, 15},           // 14
        {14,  8, 7, 23},    // 15
    
        // Inner ring (clockwise from top-left)
        {23, 17},           // 16
        {16, 18, 9},        // 17
        {17, 19},           // 18
        {18, 20, 11},       // 19
        {19, 21},           // 20
        {20, 22, 13},       // 21
        {21, 23},           // 22
        {22, 16, 15}        // 23
    };
    
    public static final int[] ADJACENT_POSITION_BBS = BitBoardUtil.positionsArrayToBitBoards(ADJACENT_POSITIONS);

    int freebb;
    
    public ActionGenerator()
    {
    }
    
    public List<ByteAction> generateMoves(List<ByteAction> actions, Board board, boolean includeIntermediate)
    {
        freebb = BitBoardUtil.freeSpaces(board.bbs);
        
        //board.currentPhase = Board.updatePhase(board);
        board.awaitingCapture = 0;

        if(board.unplacedPieces[board.currentPlayer] > 0){
        	if ( board.awaitingCapture == 1)
        		return generateCaptureAction(actions, board,  1);
        	
            return generatePhase1Action(actions, board);
        } else if (board.piecesLeft[board.currentPlayer] > 3){
        	if ( board.awaitingCapture == 1)
                return generateCaptureAction(actions, board, 2);
        	
            return generatePhase2Action(actions, board);
    	} else{ 
        	if ( board.awaitingCapture == 1)
                return generateCaptureAction(actions, board, 3);
        	
            return generatePhaseFinalAction(actions, board);
    	}
        
    }

    protected List<ByteAction> generateCaptureAction(List<ByteAction> moves, Board board, int intPhase)
    {
        freebb = BitBoardUtil.freeSpaces(board.bbs);

        // Localize variables to improve performance
      //  Board board = state.board;
        byte player = board.currentPlayer;
        byte opponent = (byte)(1-player);
       //int playerbb = board.bbs[player];
       // int opponentbb = board.bbs[opponent];
        //byte[] unplacedPieces = board.unplacedPieces;
       // byte[] piecesLeft = board.piecesLeft;
        
        // Temp local variables
      //  int[] newbbs;
        
        // current player can take any of her pieces that's not in a mill
       // BoardFeatures.detectMills(board);
        int captureablebb = board.bbs[opponent] & ~BoardFeatures.detectMillsFromBB(board.bbs[opponent]);
        		//state.features.millbbs[opponent];
        
        // New rule: if all opponent's pieces are in a mill,  allow taking of
        // mill pieces as well
        if ( captureablebb == 0 )
            captureablebb = board.bbs[opponent];            
                
        // calculate number of pieces left after capturing one from opponent
       // board.piecesLeft[opponent] = (byte) (board.piecesLeft[opponent] - 1);

        // for each capturable piece...
        for (int bb = captureablebb, mask = 1, pos = 0; 
             bb != 0; 
             bb >>>= 1, mask <<= 1, pos++){
            if ((bb & 1) != 0)
            {
            	//System.out.println("Catturabile:" +pos);
//                newbbs = new int[2];
//                newbbs[player] = playerbb;
//                newbbs[opponent] = opponentbb ^ mask;
            	ByteAction temp = new ByteAction();
            	temp.remove = (byte) pos;
            	//System.out.println("Board: "+board);
            	//System.out.println("Capturable action temp: "+temp.remove);
                moves.add(temp);
               // temp = null;
            }
        }
        board.awaitingCapture = 0;
        return moves;
    }
    
    protected List<ByteAction> generatePhase1Action(List<ByteAction> moves, Board board)
    {
        freebb = BitBoardUtil.freeSpaces(board.bbs);

    	byte awaitingCapture = 0;
        // Localize fields to improve performance
    	byte player = board.currentPlayer;
       // Board board = state.board;
        int newbb;
        byte opponent = (byte)(1-player);

        
        // for each space which is not empty
        for ( int bb=freebb, mask=1, pos=0; 
              bb!=0; 
              bb>>>=1, mask<<=1, pos++ )
        if ( (bb & 1)!=0 )
        {
//            newbbs = new int[2];
            newbb = 0;
            newbb = board.bbs[player] ^ mask;
            
            if(hasNewMills(board.bbs[player],newbb)){
//            	System.out.println("HA MILL");
//            	Board deb = new Board();
//            	deb.bbs[0]=newbb;
//            	//System.out.println("newbb "+deb);
            	awaitingCapture = 1;}
            else{
            	//System.out.println("NON HA MILL");
            	awaitingCapture = 0;}

            //provo a tenere solo le azioni
            ByteAction temp = new ByteAction();
            temp.put = (byte) pos;
            temp.phase = 1;
           // System.out.println("Temp fase1: "+temp);
            expandAndPut(moves, board, temp, 1, awaitingCapture);
            temp = null;
        }

        return moves;
    }

    //private void expandAndPut(ActionsMap moves, OurState state, int intPhase)
    private void expandAndPut(List<ByteAction> moves,Board board, ByteAction move, int intPhase, byte awaitingCapture)
    {
        freebb = BitBoardUtil.freeSpaces(board.bbs);

        if ( awaitingCapture==0 )
        {
            moves.add(move);
        }
        else
        {
           // expandCaptureMoves(moves, state, intPhase);
            expandCaptureMoves(moves, board, move, intPhase);
        }
    }

   // private void expandCaptureMoves(ActionsMap moves, OurState state, int intPhase)
    private void expandCaptureMoves(List<ByteAction> moves, Board board, ByteAction move, int intPhase)
    {
    	
    	List<ByteAction> captureActionMap = new ArrayList<ByteAction>(9);
        //il metodo qui sotto mi riempie captureActionMap 
        generateCaptureAction(captureActionMap, board, intPhase);
//        System.out.println("Capture action map "+captureActionMap);
//        System.out.println("Della board "+board);
       // OurState capturedState;
       // Action actionWithRemove;
        for (ByteAction captureMove : captureActionMap)
        {//System.out.println("Capture move: remove = "+captureMove.remove );
        	ByteAction toAdd = new ByteAction();
        	toAdd.put = move.put;
        	toAdd.from = move.from;
        	toAdd.to = move.to;
        	toAdd.remove = captureMove.remove;
        	toAdd.phase = (byte) intPhase;

         //System.out.println("Move: remove = "+captureMove.remove );
        	//move.phase = (byte) intPhase;
            moves.add(toAdd);
            //System.out.println("aggiungo "+toAdd);
           // System.out.println("lista: "+moves);
        }
    }     
    
    
    protected List<ByteAction> generatePhase2Action(List<ByteAction> moves, Board board)
    {
        freebb = BitBoardUtil.freeSpaces(board.bbs);

    	byte awaitingCapture = 0;
        // Localize fields to improve performance
    	byte player = board.currentPlayer;
        //Board board = state.board;
        byte opponent = (byte)(1-player);
        //int playerbb = board.bbs[player];
       // int opponentbb = board.bbs[opponent];
        //byte[] unplacedPieces = board.unplacedPieces;
        //byte[] piecesLeft = board.piecesLeft;
       // int freebb = BitBoardUtil.freeSpaces(board.bbs);
        		//state.features.freebb;  

        // Temp variables
       // int[] newbbs;
        int newbb;
        
        // for each space which contains current players piece
        for ( int bb=board.bbs[player], from=0, mask=1; 
              bb!=0; 
              bb>>>=1, mask<<=1, from++ )
        if ( (bb & 1)!=0 )
        {
            // pieces can move to any space that is adjacent and free 
            int adjbb = ActionGenerator.ADJACENT_POSITION_BBS[from] & freebb;
            
            // now create a new move for each one
            for (int destMask=1, to=0; 
                 adjbb!=0; 
                 adjbb>>>=1, destMask<<=1, to++)
            if ( (adjbb & 1) != 0 )
            {
            	newbb = 0;
//                newbbs = new int[2];
//                newbbs[player] = playerbb ^ (mask | destMask);
//                newbbs[opponent] = opponentbb;
            	newbb = board.bbs[player] ^ (mask | destMask);
            	
            	if(hasNewMills(board.bbs[player],newbb))
            		awaitingCapture = 1;
            	else
            		awaitingCapture = 0;
            	
            	ByteAction temp = new ByteAction((byte) 2);
            	temp.from = (byte) from;
            	temp.to = (byte) to;
            	temp.phase = 2;
            	
                expandAndPut(moves, board, temp, 2, awaitingCapture);
            }
        }            
        
        return moves;
    }

    protected List<ByteAction> generatePhaseFinalAction(List<ByteAction> moves, Board board)
    {
        freebb = BitBoardUtil.freeSpaces(board.bbs);

        // Localize fields to improve performance
    	byte player = board.currentPlayer;
       // Board board = state.board;
        byte opponent = (byte)(1-player);
        int newbb;
        byte awaitingCapture = 0;
        //int playerbb = board.bbs[player];
        //int opponentbb = board.bbs[opponent];
       // byte[] unplacedPieces = board.unplacedPieces;
        //byte[] piecesLeft = board.piecesLeft;
        //int freebb = state.features.freebb;  
        
        // Temp variables
       // Board newBoard;
        //BoardFeatures newFeatures;
       // OurState newState;
       // int[] newbbs;
       // Action action;

        // for each space which contains current players piece
        for (int bb = board.bbs[player], from = 0, mask = 1; 
             bb != 0; 
             bb >>>= 1, mask <<= 1, from++)
        if ((bb & 1) != 0)
        {
            // pieces can jump to any space that is free
            for (int bb2 = freebb, destMask = 1, to = 0; 
                 bb2 != 0; 
                 bb2 >>>= 1, destMask <<= 1, to++)
            if ((bb2 & 1) != 0)
            {
            	newbb = 0;
//                newbbs = new int[2];
//                newbbs[player] = playerbb ^ (mask | destMask);
//                newbbs[opponent] = opponentbb;
                newbb = board.bbs[player] ^ (mask | destMask);
                
                if(hasNewMills(board.bbs[player], newbb))
                	awaitingCapture = 1;
                else
                	awaitingCapture = 0;
                	
                ByteAction temp = new ByteAction((byte) 3);
                temp.from = (byte) from;
                temp.to = (byte) to;
                temp.phase = 3;

                expandAndPut(moves, board, temp, 3, awaitingCapture);
            }
        }
        
        return moves;
    }    
    
    public int getNewMills(int bb, int newbb)
    {
        return BoardFeatures.detectMillsFromBB(newbb) & ~BoardFeatures.detectMillsFromBB(bb); 
    }
    
    public boolean hasNewMills(int bb, int newbb)
    {
        return getNewMills(bb, newbb) > 0;
    }
    
//    public boolean hasOnlyMills(int player)
//    {
//        return features.millbbs[player] == board.bbs[player];
//    }
}
