package de.tautenhahn.dependencies.rest;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import org.junit.Test;


/**
 * Unit test for the server. Requires that the server is not running in parallel.
 * 
 * @author TT
 */
public class TestServer
{

  /**
   * Simple smoke test until other routes are up.
   */
  @Test
  public void getView()
  {
    String result = new Server.JsonTransformer().render(Server.displayGraph(null, null));
    assertThat("view result", result, containsString("DisplayableDiGraph"));
  }
}
