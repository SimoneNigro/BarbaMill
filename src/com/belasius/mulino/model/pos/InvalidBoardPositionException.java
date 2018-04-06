/*
 * Created on Mar 7, 2006
 *
 * (c) 2005 FAO of the UN
 */
package com.belasius.mulino.model.pos;

/**
 * Thrown when a position not on the board or between spaces
 * is requested.
 *
 * @author G. Miceli
 */
public class InvalidBoardPositionException extends Exception
{    
    private static final String MESSAGE = "Illegal position: ";

    public InvalidBoardPositionException(String pos)
    {
        super(MESSAGE+pos);
    }
}
