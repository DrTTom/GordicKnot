package de.tautenhahn.dependencies.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;


/**
 * TODO: get the logic clean!
 *
 * @author TT
 */
public class Filter
{

  private final Collection<Pattern> ignoredClassNames = new ArrayList<>();

  private final Collection<Pattern> ignoredSources = new ArrayList<>();

  private final Collection<Pattern> focus = new ArrayList<>();


  /**
   * Creates an instance which by default focuses on all the classes given as class files in directories and
   * ignores java native classes.
   */
  public Filter()
  {
    ignoredClassNames.add(Pattern.compile("java\\..*"));
    ignoredClassNames.add(Pattern.compile(".*module-info"));
    ignoredClassNames.add(Pattern.compile("META-INF\\..*"));
    ignoredSources.add(Pattern.compile(".*/jre/lib/.*"));
    ignoredSources.add(Pattern.compile(".*/build/resources/.*"));
    ignoredSources.add(Pattern.compile(".*/configuration/org.eclipse.*/\\.cp"));
    focus.add(Pattern.compile("dir:.*"));
  }

  /**
   * Adds a regular expression for fully qualified class names to ignore. Matching classes are not analyzed,
   * dependencies to those classes are taken for granted.
   *
   * @param regex
   */
  public void addIgnoredClassName(String regex)
  {
    ignoredClassNames.add(Pattern.compile(regex));
  }

  /**
   * Returns true if name denotes a source not to be parsed.
   *
   * @param name
   */
  public boolean isIgnoredSource(String name)
  {
    return ignoredSources.stream().anyMatch(s -> s.matcher(name).matches());
  }

  /**
   * Returns true if name is the class name of an ignored class. Dependency to such classes are ignored as
   * well and taken for granted.
   *
   * @param name
   */
  public boolean isIgnoredClass(String name)
  {
    return ignoredClassNames.stream().anyMatch(p -> p.matcher(name).matches());
  }

  /**
   * Returns true if name denotes an element which should undergo all the analyzing procedures, namely
   * something with source code in the analyzed project.
   *
   * @param name name of the node
   */
  public boolean isInFocus(String name)
  {
    return focus.stream().anyMatch(p -> p.matcher(name).matches());
  }

  @Override
  public int hashCode()
  {
    return ignoredSources.hashCode() + 3 * ignoredClassNames.hashCode() + 9 * focus.hashCode();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    if (obj == null || getClass() != obj.getClass())
    {
      return false;
    }
    Filter other = (Filter)obj;
    return ignoredSources.equals(other.ignoredSources) && ignoredClassNames.equals(other.ignoredClassNames)
           && focus.equals(other.focus);
  }
}
