/*
 * Created on Mar 9, 2006
 *
 * (c) 2005 FAO of the UN
 */
package com.belasius.mulino.model.pos;


/**
 * Represents and validates the column/row representation of board positions,
 * and provides conversion to/from board position numbers.  Columns are 
 * indicated by the letters A-G, and rows 1-7 as follows:
 *
 <p><blockquote><pre>
     A   B   C   D   E   F   G
 1   +-----------+-----------+   
     |           |           |   
 2   |   +-------+-------+   |
     |   |       |       |   |
 3   |   |   +---+---+   |   |
     |   |   |       |   |   |
 4   +---+---+       +---+---+
     |   |   |       |   |   |
 5   |   |   +---+---+   |   |
     |   |       |       |   |
 6   |   +-------+-------+   |
     |           |           |
 7   +-----------+-----------+
 </pre></blockquote></p>
 
 * @author G. Miceli
 */
public class ColRowBoardPosition implements BoardPosition
{
    private char col;
    private char row;
    
    public ColRowBoardPosition()
    {
    }

    public ColRowBoardPosition(byte position)
    {
        setPosition(position);
    }

    public ColRowBoardPosition(String pos)
    {
        if (pos.length() != 2)
            pos="  ";
        
        setColRowPosition(pos);
    }

    public ColRowBoardPosition(char col, char row)
    {
        setColRowPosition(col, row);
    }
    
    public void setColRowPosition(char col, char row)
    {
        this.col = col;
        this.row = row;
    }
    
    public void setColRowPosition(String pos)
    {
        pos = pos.toUpperCase();
        setColRowPosition(pos.charAt(0), pos.charAt(1));
        
    }
    
    public boolean isValid()
    {
        return col >= 'A' && col <= 'G' && row >= '1' && row <= '7' && toXYBoardPosition().isValid();
    }
    
    public XYBoardPosition toXYBoardPosition()
    {
        return new XYBoardPosition((byte)(col-'A'-3), (byte)(-row-'7'+3));
    }

    public byte getPosition()
    {
        return toXYBoardPosition().getPosition();
    }

    public void setPosition(byte position)
    {
        XYBoardPosition xy = new XYBoardPosition(position);        
        ColRowBoardPosition cr = xy.toColRowBoardPosition();
        this.col = cr.getCol();
        this.row = cr.getRow();        
    }

    public void validate() throws InvalidBoardPositionException
    {
        if ( !isValid() )
            throw new InvalidBoardPositionException(Character.toString(col)+Character.toString(row));
    }

    public void setColRow(char col, char row)
    {
        this.col = col;
        this.row = row;        
    }
    
    public char getCol()
    {
        return col;
    }

    public void setCol(char col)
    {
        this.col = col;
    }

    public char getRow()
    {
        return row;
    }

    public void setRow(char row)
    {
        this.row = row;
    }
   
    public String toString()
    {
        return ""+col+row;
    }
}
