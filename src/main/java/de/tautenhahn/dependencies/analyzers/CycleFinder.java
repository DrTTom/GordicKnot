package de.tautenhahn.dependencies.analyzers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import de.tautenhahn.dependencies.analyzers.DiGraph.IndexedNode;


/**
 * Applies the algorithm of Tarjan to find components of strong connectivity in a graph.
 *
 * @author TT
 */
public class CycleFinder
{

  private final int[] foundInStep;

  private final int[] earliestFoundSuccessor;

  private int maxUsedIndex;

  private final boolean[] isOnStack;

  private final IndexedNode[] stack;

  private int stackSize;

  private final List<List<IndexedNode>> strongComponents = new ArrayList<>();

  /**
   * Returns the components of strong connectivity sorted by descending size.
   */
  public List<List<IndexedNode>> getStrongComponents()
  {
    return strongComponents;
  }

  /**
   * Returns the subgraph induced by all nodes which are on cycles.
   */
  public DiGraph createGraphFromCycles()
  {
    Collection<DiGraph> parts = strongComponents.stream()
                                                .filter(l -> l.size() > 1)
                                                .map(DiGraph::new)
                                                .collect(Collectors.toList());
    return new DiGraph(parts.toArray(new DiGraph[0]));
  }

  /**
   * Creates instance and runs analysis
   *
   * @param graph Graph to analyze.
   */
  public CycleFinder(DiGraph graph)
  {
    foundInStep = new int[graph.getAllNodes().size()];
    earliestFoundSuccessor = new int[foundInStep.length];
    isOnStack = new boolean[foundInStep.length];
    stack = new IndexedNode[foundInStep.length];

    for ( IndexedNode node : graph.getAllNodes() )
    {
      if (foundInStep[node.getIndex()] == 0)
      {
        tarjan(node);
      }
    }
    Collections.sort(strongComponents, (a, b) -> b.size() - a.size());
  }

  private void tarjan(IndexedNode inode)
  {
    int node = inode.getIndex();
    foundInStep[node] = ++maxUsedIndex;
    earliestFoundSuccessor[node] = maxUsedIndex;
    push(inode);
    for ( IndexedNode succN : inode.getSuccessors() )
    {
      int succ = succN.getIndex();
      if (foundInStep[succ] == 0)
      {
        tarjan(succN);
        earliestFoundSuccessor[node] = Math.min(earliestFoundSuccessor[node], earliestFoundSuccessor[succ]);
      }
      else if (isOnStack[succ])
      {
        earliestFoundSuccessor[node] = Math.min(earliestFoundSuccessor[node], earliestFoundSuccessor[succ]);
      }
    }
    if (earliestFoundSuccessor[node] == foundInStep[node])
    {
      List<IndexedNode> component = new ArrayList<>();
      strongComponents.add(component);
      IndexedNode other = null;
      do
      {
        other = pop();
        component.add(other);
      }
      while (other != inode);
    }
  }

  private void push(IndexedNode node)
  {
    stack[stackSize++] = node;
    isOnStack[node.getIndex()] = true;
  }

  private IndexedNode pop()
  {
    IndexedNode node = stack[--stackSize];
    isOnStack[node.getIndex()] = false;
    return node;
  }
}
