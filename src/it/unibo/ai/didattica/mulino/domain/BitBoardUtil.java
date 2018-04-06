/*
 * Created on Mar 9, 2006
 *
 * (c) 2005 FAO of the UN
 */
package it.unibo.ai.didattica.mulino.domain;

import java.util.HashMap;
import java.util.Set;

import it.unibo.ai.didattica.mulino.actions.Action;
import it.unibo.ai.didattica.mulino.actions.ByteAction;


import it.unibo.ai.didattica.mulino.domain.State.Checker;


/**
 * Tools for manipulating internal representation of 
 * Mulino bitboards.
 * 
 * @author G. Miceli
 * @see com.belasius.mulino.model.Board
 */
public class BitBoardUtil
{
    //Elenco posizioni usate dallo State del prof
    static Set<String> keys = null;
	
    public static int positionMask(int position)
    {
        return 1 << position;
    }

    public static boolean isSet(int bb, int position)
    {
        return (bb & BitBoardUtil.positionMask(position))>0;
    }

    public static int set(int bb, int position)
    {
        return bb | BitBoardUtil.positionMask(position);
    }
    
    public static int applyMove(int bb, int moveMask)
    {
        return bb ^ moveMask;        
    }

    public static int toggle(int bb, int position)
    {
        return applyMove(bb, positionMask(position));
    }

    public static int[] positionsArrayToBitBoards(int[][] src)
    {
    	int[] dest = new int[src.length];
        
        for (byte i = 0; i < src.length; i++)
            dest[i] = BitBoardUtil.positionsToBitBoard(src[i]);
        
        return dest;
    }
        
    public static int positionsToBitBoard(int[] ps)
    {
        int bb = 0;
        for (byte j = 0; j < ps.length; j++)
        {                
            bb = BitBoardUtil.set(bb, ps[j]);
        }
        return bb;
    }

    public static int freeSpaces(int[] bbs)
    {
        return ~(bbs[0] | bbs[1]) & 0xFFFFFF;
    }

    /**
     * Rotate the bitboard clockwise 90 degrees by rotating each 
     * byte left two bits.
     * 
     */
    public static int rotateClockwise(int bb)
    {
        return ((bb & 0x3F3F3F) << 2) | ((bb & 0xC0C0C0) >>> 6);
    }

    /**
     * Turn bitboard inside-out (swap inner and outer rings)
     * by reversing the first and last bytes
     * 
     */
    public static int insideOut(int bb)
    {
        return ((bb & 0xFF0000) >>> 16) | (bb & 0x00FF00) | ((bb & 0x0000FF) << 16);
    }

    /**
     * Flip the bitboard vertically by reversing the order of
     * the bits in each byte and rotating each byte right 1 bit. 
     */
    public static int vflip(int bb)
    {
       bb = ((bb & 0x010101) << 7) |
            ((bb & 0x020202) << 5) |
            ((bb & 0x040404) << 3) |
            ((bb & 0x080808) << 1) |
            ((bb & 0x101010) >> 1) |
            ((bb & 0x202020) >> 3) |
            ((bb & 0x404040) >> 5) |
            ((bb & 0x808080) >> 7);
        
        return ((bb & 0x010101) << 7) | ((bb & 0xFEFEFE) >>> 1); 
        
    }
    
    public static byte strToByte(String position){
        /*
         *  
         A   B   C   D   E   F   G
     7   1-----------2-----------3   
         |           |           |   
     6   |   9------10------11   |
         |   |       |       |   |
     5   |   |  17--18--19   |   |
         |   |   |       |   |   |
     4   8--16--24      20--12---4
         |   |   |       |   |   |
     3   |   |  23--22--21   |   |
         |   |       |       |   |
     2   |  15------14------13   |
         |           |           |
     1   7-----------6-----------5
         * 
         * */
    	//in realtà parte da 0!!!!!!!!!!
    	    switch (position.toLowerCase()) {
    		    case "a1":  return 6;
    		    case "a4":  return 7;
    		    case "a7":  return 0;
    		    case "b2":  return 14;
    		    case "b4":  return 15;
    		    case "b6":  return 8;
    		    case "c3":  return 22;
    		    case "c4":  return 23;
    		    case "c5":  return 16;
    		    case "d1":  return 5;
    		    case "d2":  return 13;
    		    case "d3":  return 21;
    		    case "d5":  return 17;
    		    case "d6":  return 9;
    		    case "d7":  return 1;
    		    case "e3":  return 20;
    		    case "e4":  return 19;
    		    case "e5":  return 18;
    		    case "f2":  return 12;
    		    case "f4":  return 11;
    		    case "f6":  return 10;
    		    case "g1":  return 4;
    		    case "g4":  return 3;
    		    case "g7":  return 2;
    	    }   
    	    System.err.println("Unknown position: " + position);
    	    return 30;
        }
        public static String byteToStr(byte position){
        	
        	switch(position){
    		    case 6:  return "a1";
    		    case 7:  return "a4";
    		    case 0:  return "a7";
    		    case 14:  return "b2";
    		    case 15:  return "b4";
    		    case 8:  return "b6";
    		    case 22:  return "c3";
    		    case 23:  return "c4";
    		    case 16:  return "c5";
    		    case 5:  return "d1";
    		    case 13:  return "d2";
    		    case 21:  return "d3";
    		    case 17:  return "d5";
    		    case 9:  return "d6";
    		    case 1:  return "d7";
    		    case 20:  return "e3";
    		    case 19:  return "e4";
    		    case 18:  return "e5";
    		    case 12:  return "f2";
    		    case 11:  return "f4";
    		    case 10:  return "f6";
    		    case 4:  return "g1";
    		    case 3:  return "g4";
    		    case 2:  return "g7";
    		}   
    		System.err.println("Unknown position: " + position);
    		return "";
    	    	
        }
        
        public static Board setState(State state, Checker player){
        
        int[] res_bbs = new int[] { 0, 0 };
        byte[] res_unplacedPieces = new byte[] { 9, 9 };
        byte[] res_piecesLeft = new byte[] { 9, 9 };
        
        //Board prof
        HashMap<String, State.Checker> board_prof = state.getBoard();
                       	
        State.Checker cell_color;
        	
        for (String s : State.positions){
        	//System.out.println("Provo a settare la posizione "+s);
        	byte which_board = 0;    	
        	//0 è la mia, 1 dell'avversario
        	cell_color = board_prof.get(s);
        	
        	if(cell_color != player && cell_color != State.Checker.EMPTY){
        		which_board = 1;
        	}
        	if(cell_color != State.Checker.EMPTY){
        		res_bbs[which_board] = BitBoardUtil.set(res_bbs[which_board], (int) BitBoardUtil.strToByte(s));
        	}
        }

        if(player == State.Checker.WHITE){
            res_piecesLeft[0] = (byte) state.getWhiteCheckersOnBoard();
            res_piecesLeft[1] = (byte) state.getBlackCheckersOnBoard();
            res_unplacedPieces[0] = (byte) state.getWhiteCheckers();
            res_unplacedPieces[1] = (byte) state.getBlackCheckers();
        }
        else if(player == State.Checker.BLACK){
            res_piecesLeft[1] = (byte) state.getWhiteCheckersOnBoard();
            res_piecesLeft[0] = (byte) state.getBlackCheckersOnBoard();
            res_unplacedPieces[1] = (byte) state.getWhiteCheckers();
            res_unplacedPieces[0] = (byte) state.getBlackCheckers();
        }

        state = null;
        board_prof = null;
        System.gc();
        
    	return new Board(res_bbs, res_unplacedPieces, res_piecesLeft);
        	
        }
        
        public static Board applyAction(Board previous, ByteAction action){
        	
        	byte put, from, to, remove;
        	        	
        	switch(action.phase){
        	
	        	case 1:
	        		put = action.put;
	        		remove = action.remove;
	        		
	        		previous.bbs[previous.currentPlayer] = set(previous.bbs[previous.currentPlayer], put);

	        		if(remove != 99){
	        			previous.bbs[1-previous.currentPlayer] = toggle(previous.bbs[1-previous.currentPlayer], remove);
	        			previous.piecesLeft[1-previous.currentPlayer] -= 1;
	        		}
							
	        		previous.unplacedPieces[previous.currentPlayer] -= 1;
	        		
	        		break;
	        	case 2:
	        		from = action.from;
	        		to = action.to;
	        		remove = action.remove;
	        		
	        		previous.bbs[previous.currentPlayer] = set(previous.bbs[previous.currentPlayer], to);
	        		previous.bbs[previous.currentPlayer] = toggle(previous.bbs[previous.currentPlayer], from);

	        		if(remove != 99){
	        			previous.bbs[1-previous.currentPlayer] = toggle(previous.bbs[1-previous.currentPlayer], remove);
	        			previous.piecesLeft[1-previous.currentPlayer] -= 1;
	        		}
	        		
	        		break;
	        	case 3:
	        		from = action.from;
	        		to = action.to;
	        		remove = action.remove;
	        		
	        		previous.bbs[previous.currentPlayer] = set(previous.bbs[previous.currentPlayer], to);
	        		previous.bbs[previous.currentPlayer] = toggle(previous.bbs[previous.currentPlayer], from);

	        		if(remove != 99){
	        			previous.bbs[1-previous.currentPlayer] = toggle(previous.bbs[1-previous.currentPlayer], remove);
	        			previous.piecesLeft[1-previous.currentPlayer] -= 1;
	        		}
	        		break;        	
        	}
        	
    		previous.currentPlayer = (byte) (1-previous.currentPlayer);

    		previous.currentPhase = Board.updatePhase(previous);
    		
        	return previous;
        }
        
        public static Board revertAction(Board previous, ByteAction action){
        	
        	byte put, from, to, remove;
        	
    		previous.currentPlayer = (byte) (1-previous.currentPlayer);
        	
        	switch(action.phase){
        	
	        	case 1:
	        		put = action.put;
	        		remove = action.remove;
	        		
	        		previous.bbs[previous.currentPlayer] = toggle(previous.bbs[previous.currentPlayer], put);

	        		if(remove != 99){
	        			previous.bbs[1-previous.currentPlayer] = set(previous.bbs[1-previous.currentPlayer], remove);
	        			previous.piecesLeft[1-previous.currentPlayer] += 1;
	        		}
							
	        		previous.unplacedPieces[previous.currentPlayer] += 1;
	        		
	        		break;
	        	case 2:
	        		from = action.from;
	        		to = action.to;
	        		remove = action.remove;
	        		
	        		previous.bbs[previous.currentPlayer] = toggle(previous.bbs[previous.currentPlayer], to);
	        		previous.bbs[previous.currentPlayer] = set(previous.bbs[previous.currentPlayer], from);

	        		if(remove != 99){
	        			previous.bbs[1-previous.currentPlayer] = set(previous.bbs[1-previous.currentPlayer], remove);
	        			previous.piecesLeft[1-previous.currentPlayer] += 1;
	        		}
	        		
	        		break;
	        	case 3:
	        		from = action.from;
	        		to = action.to;
	        		remove = action.remove;
	        			        		
	        		previous.bbs[previous.currentPlayer] = toggle(previous.bbs[previous.currentPlayer], to);
	        		previous.bbs[previous.currentPlayer] = set(previous.bbs[previous.currentPlayer], from);

	        		if(remove != 99){
	        			previous.bbs[1-previous.currentPlayer] = set(previous.bbs[1-previous.currentPlayer], remove);
	        			previous.piecesLeft[1-previous.currentPlayer] += 1;
	        		}
	        		break;        	
        	}
        	
    		previous.currentPhase = Board.updatePhase(previous);
        	
        	return previous;
        }
}
