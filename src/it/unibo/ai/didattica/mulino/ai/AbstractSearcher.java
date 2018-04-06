package it.unibo.ai.didattica.mulino.ai;

import java.util.Observable;
import java.util.Observer;

public abstract class AbstractSearcher{

  protected int          minDepth;
  protected int          maxDepth;
  protected long         leafCount;
  protected long         nodeCount;
  
 
  public void setFixedDepth(int depth)
  {
    setMaxDepth(depth);
    setMinDepth(depth);
  }

  public void setMaxDepth(int depth)
  {
    maxDepth = depth;
  }

  public void setMinDepth(int depth)
  {
    minDepth = depth;
  }

  public long leafCount()
  {
    return leafCount;
  }

  public long nodeCount()
  {
    return nodeCount;
  }
  
}
