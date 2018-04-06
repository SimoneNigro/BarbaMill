/*
 * Created on Mar 7, 2006
 *
 * (c) 2005 FAO of the UN
 */
package com.belasius.mulino.model.pos;


/**
 * Implementations of this interface represent and convert between various
 * representations of board coordinates.  Converts board coordinates to a 
 * board position number from 0-23, representing the positions as defined by
 * class {@link com.belasius.mulino.model.Board}.  
 * <p>
 * Implementations should also validated their respective representations.
 * 
 * @author G. Miceli
 */
public interface BoardPosition
{  
	byte getPosition();    
    void setPosition(byte position);
    void validate() throws InvalidBoardPositionException;
    boolean isValid();
}

