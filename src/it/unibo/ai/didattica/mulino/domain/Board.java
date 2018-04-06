/*
 * Created on Mar 23, 2006
 *
 * (c) 2005 FAO of the UN
 */
package it.unibo.ai.didattica.mulino.domain;

import java.util.HashMap;
import java.util.Set;

import com.belasius.mulino.model.pos.ColRowBoardPosition;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

/**
 * Essential Mulino board state.  The board state is defined as the position
 * of the players' pieces, the number of pieces each player has left to place on the
 * board, and the number of uncaptured pieces for each player.
 * <p>
 * Given then current turn number, identical scores, analyses, and move successors 
 * should be able to be produced based on the state defined by this class.
 * <p>
 * Since there will be a lot of these produced, it's important that the representation occupy 
 * as little space as possible.  To optimize performance, the state of the pieces 
 * on the board is represented by two, 24-bit bitboards (one for each player).  Each bitboard
 * has a bit SET (1) if the respective player has a piece at that position, or UNSET (0) if not.  
 * <p>
 * The bits are ordered with the lowest order bit being at A1, or "the origin", and proceeding
 * clockwise around the outer ring.  These 8 positions occupy the low-order byte.  The same
 * is done with the middle ring starting from B2 for the next higher order byte, and then for 
 * the inner ring starting at C3 to fill the high-order bit.  
 * <p>
 * The correspondence of board positions to bits in the 24-bit value are as follows:
 * 
<p><blockquote><pre>

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
 
 </pre></blockquote></p>
 *
 * This representation allows fast access, comparison, and board rotations 
 * and transformations to be done by doing bit manipulations.
 * <p>
 * This class also provides support for equivalence transformations by 
 * relating equivalent boards through rotations and other transforms.
 *
 * @author G. Miceli
 */
public class Board implements Cloneable
{
    /** Two bitboards containing each player's respective pieces (see class description) */  
    public int[] bbs;
    /** Number of pieces waiting to be placed on the board */  
    public byte[] unplacedPieces;
    /** Number of uncaptured pieces */  
    public byte[] piecesLeft;

    public byte currentPlayer;
    public byte awaitingCapture;
    public byte currentPhase;
   
    public Board()
    {
        bbs = new int[] { 0, 0 };
        unplacedPieces = new byte[] { 9, 9 };
        piecesLeft = new byte[] { 9, 9 };
        currentPhase = 0;
        awaitingCapture = 0;
        currentPhase = 0;
    }

    public Board(int[] bbs, byte[] unplacedPieces, byte[] piecesLeft)
    {
        this.bbs = bbs;
        this.unplacedPieces = unplacedPieces;
        this.piecesLeft = piecesLeft;
    }
    
        
    public boolean isPieceAt(byte pos, byte player)
    {
        return BitBoardUtil.isSet(bbs[player], pos);
    }

    @Override
    public Board clone()
    {
        return new Board(bbs.clone(), unplacedPieces.clone(), piecesLeft.clone());
    }
    
    @Override
    public String toString()
    {
        StringBuffer sb0 = new StringBuffer();
        ColRowBoardPosition pos = new ColRowBoardPosition();
        for (byte i = 0; i < 24; i++)
        {
            if ( isPieceAt(i, (byte) 0) )
            {
                if ( sb0.length() > 0 )
                    sb0.append(",");
                
                pos.setPosition(i);
                sb0.append(BitBoardUtil.byteToStr(i));
            }
            
        }
        sb0.insert(0, "Board 0: [");
        sb0.append("] ");
        
        StringBuffer sb1 = new StringBuffer();
        for (byte i = 0; i < 24; i++)
        {
            if ( isPieceAt(i, (byte) 1) )
            {
                if ( sb1.length() > 0 )
                    sb1.append(",");
                pos.setPosition(i);
                sb1.append(BitBoardUtil.byteToStr(i));
            }
        }
        sb1.insert(0, "Board 1: [");
        sb1.append("] ");
        sb0.append(sb1);
        sb0.append(" Unplaced: ");
        sb0.append(unplacedPieces[0]);
        sb0.append("/");
        sb0.append(unplacedPieces[1]);
        
     return sb0.toString();
        
    }

    /**
     * Return a hash code for this board state, guaranteeing that
     * equivalent boards will have the same hash code.  This is
     * a truncated hash code, however, identical hash codes returned
     * by this function does not guarantee equivalence.
     * <p>  
     *  
     * @see #equals(Object) 
     */
    public int hashCode()
    {
       long x = getTransposition();

        // Apply hashcode calculation used by class Long.hashCode();
        return (int) ( x ^ (x>>>32) );
       // return (((hash<<1) | currentPlayer) << 2) | currentPhase;

    }
    /**
     * Board equivalence transforms allow us to reduce the branching 
     * factor by a factor of SIXTEEN (16), since 
     * the transformed board states are logically equivalent to their 
     * untransformed state!  All permutations of the following 
     * three transformations are unique and can be used to create
     * equivalence states:
     * <p>
     * Rotate (x4) - Rotations pieces 0, 90, 180 and 270 degrees
     * <p>
     * Flip (x2)- Change direction of ordering of pieces.  Horizontal and
     * vertical flip are equivalent since rotation can produce either
     * from the other.
     * <p>
     * Invert (x2)- Turn the board inside out, replacing the outer ring
     * with the inner-most ring. 
     * <p>
     * The position equivalents for these transformations are:
     * (pos=0 is the origin, A1)
     * 
     * <p><blockquote><pre> 
     * Pos    90 CW   180 CW   270 CW   H. FLIP  INVERT
     * Outer ring (ring=0):
     *   0        2        4        6      23       16
     *   1        3        5        7      22       17
     *   2        4        6        0      21       18
     *   3        5        7        1      20       19
     *   4        6        0        2      19       20
     *   5        7        1        3      18       21
     *   6        0        2        4      17       22
     *   7        1        3        5      16       23
     * 
     * General formula for outer ring:
     *   n     (n+2)    (n+4)    (n+6)  (23-n)   (n+16)
     *         mod 8    mod 8    mod 8           mod 8   
     *     
     * Middle ring (ring=1):
     *   8       10       12       14      15        8     
     *   9       11       13       15      14        9
     *  10       12       14        8      13        10
     *         ...  same as outer ring + 8 ...
     *  15        9       11       13       8        15
     *
     * Inner ring (ring=2):
     *  16       18       20       22       7         0
     *         ...  same as outer ring + 16 ...
     *  23       17       19       21       0         7
     * </pre></blockquote></p>
     * 
     * All transformations can be done quickly by bit manipulations.
     * Since each ring is encoded as one byte, we can rotate the whole
     * board by rotating each byte.  
     * <p>
     * Rotation: Rotating a byte left by 2 bits (w/high bits wrapping around to lower
     * order bits) is equivalent to one 90 degree clockwise rotation.
     * (e.g. 00100001 --> 10000100 (90CW) --> 00010010 (180CW) --> 01001000 (270CW)
     * <p>
     * Vertical flip: Reversing the bits in a byte and rotating right by 1 bit
     * flips the board vertically.
     * (e.g. 00100001 --> 10000100 --> 01000010 (vert)
     * <p>
     * Reverse: The board can be reversed (turned inside-out) by swapping the first
     * and third bytes in the bitboard (inner and outer rings).  Rotation and flip 
     * can then be applied to this to obtain more equivalence classes.
     * <p>  
     * Both player's bitboards must be rotated together for this to work correctly.
     * <p>
     * Also node that the hashCode doesn't consider turn number, number of turns completed,
     * or pieces left to place/pieces left on board.  The idea is to get a unique code for
     * the configuration of the board to distinguish between identical configurations of 
     * PIECE PLACEMENT.
     * 
     * @see #equals(Object)
     */    
    @Override
    public boolean equals(Object obj)
    {        
        Board other = (Board) obj;        
        
        if ( this.unplacedPieces[0] != other.unplacedPieces[0] ||
             this.unplacedPieces[1] != other.unplacedPieces[1] ||
             this.piecesLeft[0] != other.piecesLeft[0] ||
             this.piecesLeft[1] != other.piecesLeft[1] )
            return false;
        
        int this0 = this.bbs[0];
        int this1 = this.bbs[1];
        int other0 = other.bbs[0];
        int other1 = other.bbs[1];
        
        // Try board in all rotations and directions 
        // (normal and flipped vertically)
        
        int bx0 = other0;
        int bx1 = other1;

        // Unrotated
        if ( this0 == bx0 && this1 == bx1 ) 
            return true;

        if ( this0 == BitBoardUtil.vflip(bx0) &&
             this1 == BitBoardUtil.vflip(bx1) )
            return true;
        
        // Clockwise 90 degrees
        bx0 = BitBoardUtil.rotateClockwise(bx0);
        bx1 = BitBoardUtil.rotateClockwise(bx1);
        if ( this0 == bx0 && this1 == bx1 )    
            return true;

        if ( this0 == BitBoardUtil.vflip(bx0) &&
             this1 == BitBoardUtil.vflip(bx1) )
            return true;
        
        // Clockwise 180 degrees
        bx0 = BitBoardUtil.rotateClockwise(bx0);
        bx1 = BitBoardUtil.rotateClockwise(bx1);
        if ( this0 == bx0 && this1 == bx1 )    
            return true;

        if ( this0 == BitBoardUtil.vflip(bx0) &&
             this1 == BitBoardUtil.vflip(bx1) )
            return true;
        
        // Clockwise 270 degrees
        bx0 = BitBoardUtil.rotateClockwise(bx0);
        bx1 = BitBoardUtil.rotateClockwise(bx1);
        if ( this0 == bx0 && this1 == bx1 )    
            return true;

        if ( this0 == BitBoardUtil.vflip(bx0) &&
             this1 == BitBoardUtil.vflip(bx1) )
            return true;
        
        // Now apply all of the above transforms to the 
        // same board turned INSIDE-OUT  

        bx0 = BitBoardUtil.insideOut(other0);
        bx1 = BitBoardUtil.insideOut(other1);
        
        // Unrotated
        if ( this0 == bx0 && this1 == bx1 ) 
            return true;

        if ( this0 == BitBoardUtil.vflip(bx0) &&
             this1 == BitBoardUtil.vflip(bx1) )
            return true;
        
        // Clockwise 90 degrees
        bx0 = BitBoardUtil.rotateClockwise(bx0);
        bx1 = BitBoardUtil.rotateClockwise(bx1);
        if ( this0 == bx0 && this1 == bx1 )    
            return true;

        if ( this0 == BitBoardUtil.vflip(bx0) &&
             this1 == BitBoardUtil.vflip(bx1) )
            return true;
        
        // Clockwise 180 degrees
        bx0 = BitBoardUtil.rotateClockwise(bx0);
        bx1 = BitBoardUtil.rotateClockwise(bx1);
        if ( this0 == bx0 && this1 == bx1 )    
            return true;

        if ( this0 == BitBoardUtil.vflip(bx0) &&
             this1 == BitBoardUtil.vflip(bx1) )
            return true;
        
        // Clockwise 270 degrees
        bx0 = BitBoardUtil.rotateClockwise(bx0);
        bx1 = BitBoardUtil.rotateClockwise(bx1);
        if ( this0 == bx0 && this1 == bx1 )    
            return true;

        if ( this0 == BitBoardUtil.vflip(bx0) &&
             this1 == BitBoardUtil.vflip(bx1) )
            return true;
       
        return false;
    }
    
    /**
     * Creates a hash code for this board state, guaranteeing that
     * equivalent boards will have the same hash code.  If two
     * hash codes are not equal, their respective board states are
     * not equivalent. 
     *  
     * @see #equals(Object)
     */
    /*
    public long calculateHashCode()
    {        
        // Returns the minimum hashCode found 
        int bb0 = bbs[0];
        int bb1 = bbs[1];
        
        // Start with hashcode for unrotated bitboard
        long x = (bb1 << 24) | bb0;

//        //prova senza equivalenze
//        x = (x << 1) | (long) currentPlayer;
//        //cazzo
//        x = (x << 2) | (long) currentPhase;
//        return x;
     
        // First try all rotations and directions 
        // (normal and flipped vertically)
        int bx0 = bb0;
        int bx1 = bb1;
        int br0, br1;
        
        // Unrotated flipped board
        br0 = BitBoardUtil.vflip(bx0);
        br1 = BitBoardUtil.vflip(bx1);
        x = Math.min(x, (br1 << 24) | br0);          
        
        // Rotated 90 degrees
        bx0 = BitBoardUtil.rotateClockwise(bx0);
        bx1 = BitBoardUtil.rotateClockwise(bx1);                  
        x = Math.min(x, (bx1 << 24) | bx0);  
        
        // Flipped, rotated 90 degrees
        br0 = BitBoardUtil.vflip(bx0);
        br1 = BitBoardUtil.vflip(bx1);
        x = Math.min(x, (br1 << 24) | br0);
        
        // Rotated 180 degrees
        bx0 = BitBoardUtil.rotateClockwise(bx0);
        bx1 = BitBoardUtil.rotateClockwise(bx1);
        x = Math.min(x, (bx1 << 24) | bx0);  

        // Flipped, rotated 180 degrees
        br0 = BitBoardUtil.vflip(bx0);
        br1 = BitBoardUtil.vflip(bx1);
        x = Math.min(x, (br1 << 24) | br0);  
        
        // Rotated 270 degrees
        bx0 = BitBoardUtil.rotateClockwise(bx0);
        bx1 = BitBoardUtil.rotateClockwise(bx1);
        x = Math.min(x, (bx1 << 24) | bx0);  

        // Flipped, rotated 270 degrees
        br0 = BitBoardUtil.vflip(bx0);
        br1 = BitBoardUtil.vflip(bx1);
        x = Math.min(x, (br1 << 24) | br0);     
                
        // Now try INSIDE-OUT board in all rotations and directions         
        bx0 = BitBoardUtil.insideOut(bb0);
        bx1 = BitBoardUtil.insideOut(bb1);
        
        // Inside-out, unflipped, unrotated
        x = Math.min(x, (bx1 << 24) | bx0);  
        
        // Inside-out, flipped, unrotated 
        br0 = BitBoardUtil.vflip(bx0);
        br1 = BitBoardUtil.vflip(bx1);
        x = Math.min(x, (br1 << 24) | br0);  
    
        // Rotated 90 degrees
        bx0 = BitBoardUtil.rotateClockwise(bx0);
        bx1 = BitBoardUtil.rotateClockwise(bx1);
        x = Math.min(x, (bx1 << 24) | bx0);

        // Flipped, rotated 90 degrees
        br0 = BitBoardUtil.vflip(bx0);
        br1 = BitBoardUtil.vflip(bx1);
        x = Math.min(x, (br1 << 24) | br0);  
        
        // Rotated 180 degrees
        bx0 = BitBoardUtil.rotateClockwise(bx0);
        bx1 = BitBoardUtil.rotateClockwise(bx1);
        x = Math.min(x, (bx1 << 24) | bx0);  

        // Flipped, rotated 180 degrees
        br0 = BitBoardUtil.vflip(bx0);
        br1 = BitBoardUtil.vflip(bx1);
        x = Math.min(x, (br1 << 24) | br0);  
        
        // Rotated 270 degrees
        bx0 = BitBoardUtil.rotateClockwise(bx0);
        bx1 = BitBoardUtil.rotateClockwise(bx1);
        x = Math.min(x, (bx1 << 24) | bx0);  

        // Flipped, rotated 270 degrees
        br0 = BitBoardUtil.vflip(bx0);
        br1 = BitBoardUtil.vflip(bx1);
        x = Math.min(x, (br1 << 24) | br0);  

        // Add pieces left to hashCode.  Number of unplaced pieces
        // not needed here because it is implied by board positions 
        // and number of piece left
        x = (x << 4) | piecesLeft[0];
        x = (x << 4) | piecesLeft[1];
        
//        //nostro
//        x = (x << 1) | currentPlayer;
//        
//        x = (x << 2) | currentPhase;
        
        return x;
    }    */
    public Long getTransposition() {
        long whiteBoard = this.bbs[0];
        long blackBoard = this.bbs[1];
        long phase = this.currentPhase;

        byte maxValue = -1;
        int maxPattern = 0;
        for (byte i = 0; i < 4; i++) {
            byte value = (byte)((whiteBoard >>> (6 * i)) & 0b00111111);
            if (value > maxValue) {
                maxValue = value;
                maxPattern = (1 << i);
            } else if (value == maxValue) {
                maxPattern |= (1 << i);
            }
        }
        if ((maxPattern & 0b0011) == 0b0010) {
            whiteBoard = rotateBoard90(whiteBoard);
            blackBoard = rotateBoard90(blackBoard);
        } else if ((maxPattern & 0b0110) == 0b0100) {
            whiteBoard = rotateBoard180(whiteBoard);
            blackBoard = rotateBoard180(blackBoard);
        } else if ((maxPattern & 0b1100) == 0b1000) {
            whiteBoard = rotateBoard270(whiteBoard);
            blackBoard = rotateBoard270(blackBoard);
        }

        long hash = this.currentPlayer;
        hash |= (phase << 1);
        hash |= (whiteBoard <<  3); // [0..23]  white board
        hash |= (blackBoard << 27); // [24..47] black board

        return hash;
    }
    
    private long rotateBoard90(long board) {
        long first = (board & 0b111111);
        return ((board >>> 6) | (first << 18));
    }

    private long rotateBoard180(long board) {
        long first = (board & 0b111111111111);
        return ((board >>> 12) | (first << 12));
    }

    private long rotateBoard270(long board) {
        long first = (board & 0b111111111111111111);
        return ((board >>> 18) | (first << 6));
    }
	public static byte updatePhase(Board board){
		
		if(board.unplacedPieces[0] > 0)
			return 1;
		else if(board.piecesLeft[0] > 3 &&
				board.piecesLeft[1] > 3 )
			return 2;
		else
			return 3;

		
	}
}
