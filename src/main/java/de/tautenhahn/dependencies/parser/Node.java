package de.tautenhahn.dependencies.parser;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Node in a dependency structure. , might stand for a class, package, package set, jar file or module. Nodes
 * are expected to form a single-root tree structure where all children are classes. Computations will be made
 * on the class nodes initially, the higher level nodes will follow the results. <br>
 * Note that we distinguish between the "contains" and "dependsOn" relations.<br>
 * WARNING: This is not a valid concept to model a general directed graph but specialized for analyzing java
 * dependencies.
 *
 * @author TT
 */
public abstract class Node
{

  /**
   * Defines how elements are listed and where dependencies are addressed to.
   */
  public enum ListMode
  {
    /** all children hidden, dependencies of whole subtree on this node */
    COLLAPSED,
    /** direct leaves collapsed, all other children listed separately */
    LEAFS_COLLAPSED,
    /** all children listed separately */
    EXPANDED
  }

  /**
   * Separator for the name parts.
   */
  private static final char SEPARATOR = '.';

  private final Node parent;

  private final String simpleName;

  private ListMode listMode = ListMode.EXPANDED;

  /**
   * Creates new instance.
   *
   * @param parent parent node
   * @param simpleName name of that node only, full name will be created by addig parent info
   */
  Node(Node parent, String simpleName)
  {
    this.parent = parent;
    this.simpleName = simpleName;
  }

  /**
   * @return the fully qualified name.
   */
  public String getName()
  {
    return Optional.ofNullable(parent)
                   .map(Node::getName)
                   .map(n -> n + SEPARATOR + simpleName)
                   .orElse(simpleName);
  }

  /**
   * @return the simple name.
   */
  public String getSimpleName()
  {
    return simpleName;
  }



  /**
   * @return the parent in the container structure.
   */
  public Node getParent()
  {
    return parent;
  }

  abstract Node getChildByName(String simpleChildName);

  /**
   * @return the nodes which depend on this node. In case an inner node is collapsed, it will be returned
   *         instead of its hidden children.
   */
  public abstract List<Node> getSuccessors();

  /**
   * @return the node this node depends on. Same handling of collapsed nodes.
   */
  public abstract List<Node> getPredecessors();

  /**
   * @return true if there is at least one class represented by this node and not by some expanded child node.
   */
  public abstract boolean hasOwnContent();

  /**
   * @return a list of pairs (a,b) denoting the smallest known units where a is a child of the current node, b
   *         a child of the other node, a depends on b and both a and b represent the smallest known units
   *         containing the dependency. This looks into collapsed nodes.
   * @param other node to check dependency to
   */
  public abstract List<Pair<Node, Node>> getDependencyReason(Node other);

  /**
   * Same as {@link #getDependencyReason(Node)} but returns pairs of short comprehensive strings.
   *
   * @param other node to check dependency to
   * @return short explanations
   */
  public List<Pair<String, String>> explainDependencyTo(Node other)
  {
    return getDependencyReason(other).stream()
                                     .map(p -> new Pair<>(p.getFirst().getRelativeName(this),
                                                          p.getSecond().getRelativeName(other)))
                                     .collect(Collectors.toList());
  }

  /**
   * @return a stream of children in depth-first order, ignoring parts of collapsed nodes.
   */
  public abstract Stream<Node> walkSubTree();

  /**
   * @return the mode in which children in the container structure are handled as integral part of this node
   *         or as separate nodes.
   */
  public ListMode getListMode()
  {
    return listMode;
  }

  /**
   * If parameter is true, combine this node with all its children into one collective node, collapse the
   * children as well. If false is given, consider the children as separate nodes.
   *
   * @param listMode true to collapse this node
   */
  public void setListMode(ListMode listMode)
  {
    this.listMode = listMode;
  }

  /**
   * @return sub-node specified by path, even if inside some collapsed node.
   * @param path relative to this node.
   */
  public Node find(String path)
  {
    Pair<String, String> parts = splitPath(path);
    Optional<Node> result = Optional.ofNullable(getChildByName(parts.getFirst()));
    return result.map(n -> Optional.ofNullable(parts.getSecond()).map(n::find).orElse(n)).orElse(null);
  }

  /**
   * Separates the first part of a path.
   *
   * @param path value to split
   * @return simple name and parents fully qualified name
   */
  Pair<String, String> splitPath(String path)
  {
    int pos = path.indexOf(SEPARATOR);
    return pos > 0 ? new Pair<>(path.substring(0, pos), path.substring(pos + 1)) : new Pair<>(path, null);
  }

  /**
   * Regarding the list mode, returns the node this node is currently represented by. That will be the biggest
   * collapsed container containing this node or this node itself if all ancestors are expanded.
   * 
   * @return node currently representing this one
   */
  public Node getListedContainer()
  {
    Node result = this;
    Node ancestor = getParent();
    boolean acceptLeafsCollapsed = result instanceof ClassNode;
    while (ancestor != null)
    {
      if (ancestor.listMode == ListMode.COLLAPSED
          || acceptLeafsCollapsed && ancestor.listMode == ListMode.LEAFS_COLLAPSED)
      {
        result = ancestor;
      }
      ancestor = ancestor.getParent();
      acceptLeafsCollapsed = false;
    }
    return result;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "(" + getName() + ")";
  }

  /**
   * @return the name relative to another node. Will throw exception if not in the subtree. In case of nodes
   *         are same, the simple name is returned instead because its more useful.
   * @param ancestor any ancestor node
   */
  private String getRelativeName(Node ancestor)
  {
    if (ancestor == parent || ancestor == this) // NOPMD we mean the same object
    {
      return simpleName;
    }
    if (parent == null)
    {
      throw new IllegalArgumentException(ancestor + " is not an anchestor");
    }
    return parent.getRelativeName(ancestor) + SEPARATOR + simpleName;
  }

  /**
   * @return true if this node is a container containing the other node
   * @param other any node to check
   */
  public boolean isAnchestor(Node other)
  {
    Node container = other;
    while (container != null)
    {
      if (container == this)
      {
        return true;
      }
      container = container.getParent();
    }
    return false;
  }

  /**
   * Returns a name for current node which is indented to be human-readable rather than unique within the
   * whole parsed project. More precisely, return:
   * <ul>
   * <li>short name of a directory</li>
   * <li>fully qualified class name for a class or package (omitting where it was found)</li>
   * <li>the simple file name of a jar</li>
   * </ul>
   * 
   * @return comprehensive value
   */
  public String getDisplayName()
  {
    return getName().replaceAll(".*:[^.]*\\.", "")
                    .replaceAll("[jwer]ar:", "")
                    .replaceAll("_([jwer]ar)", ".$1");
  }

}
