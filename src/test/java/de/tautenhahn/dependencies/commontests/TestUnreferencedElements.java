package de.tautenhahn.dependencies.commontests;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeThat;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runners.Parameterized;

import de.tautenhahn.dependencies.analyzers.ReferenceChecker;
import de.tautenhahn.dependencies.parser.ClassNode;
import de.tautenhahn.dependencies.parser.ContainerNode;
import de.tautenhahn.dependencies.parser.Filter;
import de.tautenhahn.dependencies.parser.GradleAdapter;
import de.tautenhahn.dependencies.parser.Node;
import de.tautenhahn.dependencies.parser.ParsedClassPath;


/**
 * Unit tests to find unreferenced elements in your class path and own classes. Override the non-final methods
 * for fine-tuning these tests. Note that we do not use {@link Parameterized} because getting the values may
 * require overwriting methods but Parameterized relies on calling a static method. <br>
 * Note that Eclipse may resolve the dependencies incorrectly making this test fail! Call from Gradle to be
 * sure.
 *
 * @author TT
 */
public class TestUnreferencedElements
{

  private final ParsedClassPath classpathTest = ParsedClassPath.getCurrentClassPath();
  // private ParsedClassPath classpathRuntime;
  // private ParsedClassPath classpathCompile;

  private final Filter filter = new Filter();

  /**
   * Asserts that there are no unused test classes in your project. Override
   * {@link #getKnownEntryClassNames()} if needed but do not cheat!
   */
  @Test
  public final void checkClassUsageTest()
  {
    checkClassUsage(classpathTest, "testRuntime", getKnownTestEntryClassNames());
  }

  /**
   * Asserts that there are no unused classes in your project.
   */
  @Test
  public final void checkClassUsageMain()
  {
    String path = getApplicationClasspath();
    assumeThat("application class path", path, notNullValue());
    checkClassUsage(new ParsedClassPath(path), "application", getKnownEntryClassNames());
  }

  /**
   * Asserts that there are no unused jars in your projects compile class path.
   */
  @Test
  public final void checkLibraryUsageCompile()
  {
    String path = getCompileClasspath();
    assumeThat("compile class path", path, notNullValue());
    checkLibraryUsage(new ParsedClassPath(path), "compile");
  }

  /**
   * Asserts that there are no unused jars in your projects compile class path.
   */
  @Test
  public final void checkLibraryUsageApplication()
  {
    String path = getApplicationClasspath();
    assumeNotNull("compile class path", path);
    checkLibraryUsage(new ParsedClassPath(path), "application", getKnownNeededLibraries());
  }

  /**
   * Asserts that there are no unused jars in your projects test runtime class path.
   */
  @Test
  public final void checkLibraryUsageTest()
  {
    checkLibraryUsage(classpathTest, "testRuntime", getKnownNeededLibraries());
  }

  /**
   * Returns an array of fully qualified names of entry classes of your project. You have to specify only
   * those classes which are not automatically recognized as entry classes. <br>
   * Example implementation targets own project.
   */
  protected String[] getKnownEntryClassNames()
  {
    return new String[]{"de.tautenhahn.dependencies.reports.CyclicDependencies"};
  }

  /**
   * Returns an array of fully qualified names of test entry classes of your project. You have to specify only
   * those classes which are not automatically recognized as entry classes. Do not specify classes of your
   * application itself because those should be referenced by some test. In most cases, this method should not
   * be overwritten.
   */
  protected String[] getKnownTestEntryClassNames()
  {
    return new String[0];
  }

  /**
   * Returns an array of names or regular expressions for jar files needed by your application but not
   * referenced by any of your classes. Logging runtime jars are a typical example for that. Moreover, gradle
   * may include its worker jar when executing tests, so ignore that one as well.
   */
  protected String[] getKnownNeededLibraries()
  {
    return new String[]{"slf4j-simple-.*\\.jar", "gradle-worker.jar"};
  }

  /**
   * Returns the compile class path. Current implementation uses Gradle, override this method if you use a
   * different build system.
   */
  protected String getCompileClasspath()
  {
    return new GradleAdapter(Paths.get("build.gradle")).getClassPath("compile");
  }

  /**
   * Returns the class path of your application. Current implementation uses Gradle, override this method if
   * you use a different build system.
   */
  protected String getApplicationClasspath()
  {
    return new GradleAdapter(Paths.get("build.gradle")).getClassPath("runtime");
  }

  private void checkClassUsage(ParsedClassPath cp, String label, String... knownEntryClasses)
  {
    ContainerNode root = ProjectCache.getScannedProject(cp, filter);
    ReferenceChecker checker = new ReferenceChecker(root, filter, cp);
    checker.addKnownNeededClasses(knownEntryClasses);
    List<String> unref = checker.getUnrefClasses()
                                .stream()
                                .map(ClassNode::getClassName)
                                .collect(Collectors.toList());
    assertThat("unreferenced classes in configuration " + label, unref, empty());
  }

  private void checkLibraryUsage(ParsedClassPath cp, String label, String... knownNeeded)
  {
    ContainerNode root = ProjectCache.getScannedProject(cp, filter);
    ReferenceChecker checker = new ReferenceChecker(root, filter, cp);
    List<String> unref = checker.getUnrefJars()
                                .stream()
                                .map(Node::getDisplayName)
                                .collect(Collectors.toList());
    Arrays.stream(knownNeeded).forEach(p -> unref.removeIf(e -> e.equals(p) || e.matches(p)));
    assertThat("unreferenced jars in configuration " + label, unref, empty());
  }

}