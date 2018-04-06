/*
 * Created on Mar 26, 2006
 *
 * (c) 2005 FAO of the UN
 */
package it.unibo.ai.didattica.mulino.ai;

/**
 * @author G. Miceli
 * @deprecated No longer used and should be removed
 */
public interface SuccessorGenerator<S extends Comparable>
{
    public Iterable<SearchNode<S>> generateSuccessors(SearchNode<S> node);
}
