/*
 * Created on Mar 24, 2006
 *
 * (c) 2005 FAO of the UN
 */
package it.unibo.ai.didattica.mulino.domain;

/**
 * Performs basic anaylsis required by the Mulino game rules,
 * including free spaces and position of mills. 
 * 
 *
 *
 * @author G. Miceli
 */
public class BoardFeatures
{
    /** Bitboard representing the spaces on the board that are free */
    public static int freebb;
    /** Bitboard representing the spaces on the board that contain mills */
    public static int[] millbbs;
    /** The number of each mills each player currently has */
    public static byte[] millcnt;
    /** The position of each player's mills.  Mills are numbered according to {@link #MILL_POSITIONS} */
    public static int[] millposbbs;

    /**
     * Positions where mills can be formed (a.k.a. "rows")
     * 
     * The 16 positions row (0-15) are ordered as defined in this array.
     * 
     */ 
    public static final int[][] MILL_POSITIONS = {

            // Mills on outer ring
            { 0, 1, 2 }, { 2, 3, 4 }, { 4, 5, 6 }, { 6, 7, 0 },

            // Mills on middle ring
            { 8, 9, 10 }, { 10, 11, 12 }, { 12, 13, 14 }, { 14, 15, 8 },

            // Mills on inner ring
            { 16, 17, 18 }, { 18, 19, 20 }, { 20, 21, 22 }, { 22, 23, 16 },

            // "Spokes" of the NMM board:

            // Mills along center vertical
            { 1, 9, 17 }, { 21, 13, 5 },

            // Mills along center horizontal
            { 7, 15, 23 }, { 19, 11, 3 } 
    };

    public static final int[] MILL_POSITION_BBS = BitBoardUtil.positionsArrayToBitBoards(MILL_POSITIONS);
    public static final byte OUTER_RING_IDX = 0;
    public static final byte MIDDLE_RING_IDX = 4;
    public static final byte INNER_RING_IDX = 8;
    public static final byte SPOKES_IDX = 12;
        
    public BoardFeatures(Board board)
    {
       // freebb = BitBoardUtil.freeSpaces(board.bbs);
       // detectMills(board);
    }

    public static void detectMills(Board board)
    {
        millbbs = new int[] { 0, 0 };
        millcnt = new byte[] { 0, 0 };
        millposbbs = new int[] { 0, 0 };
        int bb0 = board.bbs[0];
        int bb1 = board.bbs[1];
        for (byte i = 0; i < MILL_POSITION_BBS.length; i++)
        {
            int mask = MILL_POSITION_BBS[i];
            millposbbs[0]<<=1;
            millposbbs[1]<<=1;
            
            if ((bb0 & mask) == mask)
            {
                millbbs[0] |= mask;
                millcnt[0]++;
                millposbbs[0] |= 1;
            }

            if ((bb1 & mask) == mask)
            {
                millbbs[1] |= mask;
                millcnt[1]++;
                millposbbs[1] |= 1;
            }
        }
    }
    
    public static int detectMillsFromBB(int bb0)
    {
        int resmillbbs = 0;

        for (byte i = 0; i < MILL_POSITION_BBS.length; i++)
        {
            int mask = MILL_POSITION_BBS[i];
            //resmillbbs<<=1;
            
            if ((bb0 & mask) == mask)
            {
            	resmillbbs |= mask;
            }
        }
        
        return resmillbbs;
    }
}
