package de.tautenhahn.dependencies.rest;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.staticFiles;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.tautenhahn.dependencies.parser.Pair;
import spark.Request;
import spark.Response;
import spark.ResponseTransformer;


/**
 * The REST server. Supplies the graph for displaying and allows calling some commands. Current implementation
 * is for one session only.
 *
 * @author TT
 */
public class Server
{

  static PrintStream out = System.out;

  /**
   * This object should be put into the session when multiple sessions are supported.
   */
  ProjectView view;

  /**
   * Command line call.
   *
   * @param args
   */
  public static void main(String... args)
  {
    // args = new String[]{System.getProperty("java.class.path"), "Gordian Knot"};
    if (args.length == 0 || args[0].toLowerCase(Locale.ENGLISH).matches("--?h(elp)?"))
    {
      out.println("\"Gordian Knot\" dependency checker version 0.2 alpha"
                  + "\nUsage: GordianKnot <classpathToCheck> [projectName] [options]");
      return;
    }
    Pair<String, String> resolved = resolve(args[0]);
    Server instance = new Server();
    instance.view = new ProjectView(resolved.getFirst(), args.length > 1 ? args[1] : resolved.getSecond());
    instance.startSpark();
    out.println("Server started, point your browser to http://localhost:" + port() + "/index.html");
  }

  private static Pair<String, String> resolve(String value)
  {
    if (value.endsWith(".txt") && !value.contains(":"))
    {
      Path p = Paths.get(value);
      try
      {
        String path = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
        String name = String.valueOf(p.getFileName());
        name = name.substring(0, name.length() - ".txt".length());
        return new Pair<>(path, name);
      }
      catch (IOException e)
      {
        throw new IllegalArgumentException("cannot read " + value, e);
      }
    }
    return new Pair<>(value, null);
  }

  void startSpark()
  {
    staticFiles.location("frontend");
    allowCrossSiteCalls();
    JsonTransformer transformer = new JsonTransformer();
    get("view", (req, res) -> view.getDisplayableGraph(), transformer);
    get("view/name", (req, res) -> view.getProjectName());
    get("view/classpath", (req, res) -> view.getClassPath(), transformer);
    get("view/unrefReport", (req, res) -> view.getUnreferencedReport(), transformer);
    get("view/node/:id", (req, res) -> view.getNodeInfo(req.params("id")), transformer);
    get("view/arc/:id", (req, res) -> view.getArcInfo(req.params("id")), transformer);
    get("view/node/:id/listmode/:value", this::setListMode, transformer); // TODO change to put when
                                                                          // everything works!

    installFilterRoute("cycles", view::showOnlyCycles);
    installFilterRoute("none", view::showAll);
    installFilterRoute("resetListMode", view::resetListMode);
    get("view/filters/impliedBy/:id/:successors",
        (req, res) -> view.restrictToImpliedBy(Integer.parseInt(req.params("id")),
                                               Boolean.parseBoolean(req.params("successors"))),
        transformer);

  }

  private void installFilterRoute(String filter, Runnable modification) // NOPMD no threads here!
  {
    // TODO change to put when everything works!
    get("view/filters/" + filter, (req, res) -> {
      modification.run();
      return view.getDisplayableGraph();
    }, new JsonTransformer());
  }

  /**
   * @param req
   * @param res
   */
  private DisplayableDiGraph setListMode(Request req, Response res) // NOPMD must fit interface
  {
    view.setListMode(Integer.parseInt(req.params("id")), req.params("value"));
    return view.getDisplayableGraph();
  }

  /**
   * All structured output to front end is JSON.
   */
  static class JsonTransformer implements ResponseTransformer
  {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public String render(Object model)
    {
      return gson.toJson(model);
    }

  }

  static void allowCrossSiteCalls()
  {
    before((request, response) -> {
      response.header("Access-Control-Allow-Origin", "*");
      response.header("Access-Control-Request-Method", "*");
      response.header("Access-Control-Allow-Headers", "*");
      // Note: this may or may not be necessary in your particular application
      response.type("application/json");
    });

    options("/*", (request, response) -> {

      String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
      if (accessControlRequestHeaders != null)
      {
        response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
      }

      String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
      if (accessControlRequestMethod != null)
      {
        response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
      }

      return "OK";
    });
  }

}
