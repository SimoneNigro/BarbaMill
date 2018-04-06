/*
 * Created on Mar 26, 2006
 *
 * (c) 2005 FAO of the UN
 */
package it.unibo.ai.didattica.mulino.ai;

/**
 * Represents a node in the minimax search space, including the
 * evaluation score and move/state.
 * 
 * @author G. Miceli
 */
public class SearchNode<S extends Comparable> 
    implements Comparable
{
    public int score;
    public S state;
    
    public SearchNode()
    {        
    }
    
    public SearchNode(int score)
    {
        this.score = score;
    }

    public SearchNode(S state)
    {
        this.state = state;
        //quello sotto è una prova mia
        //this.score = 0;
    }

    public SearchNode(S state, int score)
    {
        this.state = state;
        this.score = score;
    }

    @Override
    public boolean equals(Object obj)
    {
        return state.equals(((SearchNode)obj).state);
    }

    @Override
    public int hashCode()
    {
        return state.hashCode();
    }

    @Override
    public String toString()
    {
        return state.toString()+" ($"+score+")";
    }

    public int compareTo(Object o)
    {
       // return state.compareTo(((SearchNode)o).state);
    	//è una mia prova per passarli ordinati al minmax
    	return Integer.compare(this.score, ((SearchNode) o).score);
    }  
}
