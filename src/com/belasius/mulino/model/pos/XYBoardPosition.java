/*
 * Created on Mar 9, 2006
 *
 * (c) 2005 FAO of the UN
 */
package com.belasius.mulino.model.pos;

import java.util.Arrays;
import java.util.List;


/**
 * Represents and validates the (X,Y) representation of board positions,
 * and provides conversion to/from board position numbers.  Used primary
 * by GUI to render and interact with the board.  Columns and rows are 
 * numbered from 0-6 as follows:

<p><blockquote><pre>
     0   1   2   3   4   5   6        
 0   +-----------+-----------+      
     |           |           |   
 1   |   +-------+-------+   |       
     |   |       |       |   |
 2   |   |   +---+---+   |   |
     |   |   |       |   |   |
 3   +---+---+       +---+---+      
     |   |   |       |   |   |
 4   |   |   +---+---+   |   |       
     |   |       |       |   |
 5   |   +-------+-------+   |      
     |           |           |
 6   +-----------+-----------+     
</pre></blockquote></p> 
 
 * @author G. Miceli
 */
public class XYBoardPosition implements BoardPosition
{     
    // Used both for validation and ordering
    static final XYBoardPosition[] POSITIONS = {
        // Outer square
    		new XYBoardPosition((byte)-3,(byte)-3), 
            new XYBoardPosition((byte)0,(byte)-3), 
            new XYBoardPosition((byte)3,(byte)-3),
            new XYBoardPosition((byte)3,(byte)0),
            new XYBoardPosition((byte)3,(byte)3), 
            new XYBoardPosition((byte)0,(byte)3), 
            new XYBoardPosition((byte)-3,(byte)3),
            new XYBoardPosition((byte)-3,(byte)0),
            // Middle square
            new XYBoardPosition((byte)-2,(byte)-2), 
            new XYBoardPosition((byte)0,(byte)-2), 
            new XYBoardPosition((byte)2,(byte)-2),
            new XYBoardPosition((byte)2,(byte)0),
            new XYBoardPosition((byte)2,(byte)2), 
            new XYBoardPosition((byte)0,(byte)2), 
            new XYBoardPosition((byte)-2,(byte)2),
            new XYBoardPosition((byte)-2,(byte)0),
            // Inner square
            new XYBoardPosition((byte)-1,(byte)-1), 
            new XYBoardPosition((byte)0,(byte)-1), 
            new XYBoardPosition((byte)1,(byte)-1),
            new XYBoardPosition((byte)1,(byte)0),
            new XYBoardPosition((byte)1,(byte)1), 
            new XYBoardPosition((byte)0,(byte)1), 
            new XYBoardPosition((byte)-1,(byte)1),
            new XYBoardPosition((byte)-1,(byte)0)
    };
    static final List<XYBoardPosition> POSITION_LIST =  Arrays.asList(POSITIONS); 
    private byte xPos;
    private byte yPos;
    
    public XYBoardPosition()
    {        
    }
    
    public XYBoardPosition(byte position)
    {
        setPosition(position);
    }

    public XYBoardPosition(byte xPos, byte yPos)
    {        
        this.xPos = xPos;
        this.yPos = yPos;
    }
    
    public byte getPosition()
    {
        return (byte) POSITION_LIST.indexOf(this);
    }

    public void setPosition(byte position)
    {
        if ( position < 0 || position > 23 )
            throw new IllegalArgumentException("Bad position: "+position);
        
        XYBoardPosition pos = POSITION_LIST.get(position);
        
        setXYPos(pos.getXPos(), pos.getYPos());
    }

    public boolean isValid()
    {
        // If the coords are out of the range of the grid, invalid!
        if ( xPos < -3 || xPos > 3 || yPos < -3 || yPos > 3 )
            return false;
        else
            return POSITION_LIST.contains(this);

    }
    
    public void validate() throws InvalidBoardPositionException
    {
        if ( !isValid() )
            throw new InvalidBoardPositionException("("+xPos+","+yPos+")");
    }
    
    public void setXYPos(byte xPos, byte yPos)
    {
        this.xPos = xPos;
        this.yPos = yPos;
    }
    
    public void setXPos(byte pos)
    {
        xPos = pos;
    }

    public void setYPos(byte pos)
    {
        yPos = pos;
    }

    public byte getXPos()
    {
        return xPos;
    }

    public byte getYPos()
    {
        return yPos;
    }

    @Override
    public int hashCode()
    {
        return (yPos+3) | ((xPos+3) << 4);
    }

    @Override
    public boolean equals(Object o)
    {
        XYBoardPosition other = (XYBoardPosition) o;
        return this.xPos==other.xPos && this.yPos==other.yPos;
    }
    @Override
    public String toString()
    {
        return "("+xPos+","+yPos+")";
    }

    public ColRowBoardPosition toColRowBoardPosition()
    {
       // return new ColRowBoardPosition((char) (xPos+3+'A'), (char) (yPos+3+'1'));
        return new ColRowBoardPosition((char) (xPos+3+'A'), (char) ('7'+yPos-3));
    }
}
