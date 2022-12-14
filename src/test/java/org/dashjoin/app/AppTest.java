package org.dashjoin.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import com.api.jsonata4java.Expression;
import com.api.jsonata4java.expressions.EvaluateException;
import com.api.jsonata4java.expressions.Expressions;
import com.api.jsonata4java.expressions.functions.FloorFunction;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Perform some checks on the App's JSON config
 */
public class AppTest {

  /**
   * configure JSON parser to allow comments and multi line strings
   */
  protected static final ObjectMapper objectMapper = new ObjectMapper()
      .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true)
      .configure(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature(), true)
      .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature(), true);;

  /**
   * allowed values for widget field
   */
  List<String> allowedWidget = Arrays.asList("button", "links", "dj-toolbar", "dj-table-metadata",
      "chart", "card", "expansion", "edit", "all", "create", "container", "grid", "display",
      "${pk1}", "text", "activity-status", "upload", "icon", "spacer", "layout-edit-switch",
      "search", "search-result", "toolbar", "table", "queryeditor", "editRelated", "markdown",
      "page", "tree", "variable", "sidenav-switch", "map", "html",
      /* json schema form */ "textarea", "select", "date");

  /**
   * dashjoin JSONata functions
   */
  List<String> functions = Arrays.asList("$all", "$read", "$create", "$update", "$traverse",
      "$delete", "$query", "$queryGraph", "$call", "$incoming", "$trigger", "$djRoles",
      "$djVersion", "$djGetDatabases", "$djGetDrivers", "$djGetFunctions", "$echo",
      "$alterColumnTrigger", "$alterTableTrigger", "$doc2data", "$djSubscription", "$crawl", "$ls",
      "$openText", "$openJson");

  /**
   * syntax check of the files in the model folder
   */
  @Test
  public void testJson() throws Exception {
    for (File file : new File("model").listFiles()) {
      for (File item : file.listFiles()) {
        // parse JSON
        JsonNode tree = objectMapper.readTree(item);

        // check widget values
        JsonNode widgets = Expression.jsonata("**.widget").evaluate(tree);
        if (widgets != null) {
          for (JsonNode widget : widgets instanceof TextNode ? Arrays.asList(widgets) : widgets) {
            Assert.assertTrue("Illegal widget: " + widget, allowedWidget.contains(widget.asText()));
          }
        }

        // parse and dry-run JSONata expressions
        for (String x : new String[] {"if", "context", "display", "expression", "onClick",
            "before-create", "after-create", "before-update", "after-update", "before-delete",
            "after-delete"}) {
          JsonNode expressions = Expression.jsonata("**." + x).evaluate(tree);
          if (expressions != null) {
            for (JsonNode expression : expressions instanceof TextNode ? Arrays.asList(expressions)
                : expressions) {
              // parse expression
              parse(expression.asText());
            }
          }
        }
      }
    }
  }

  /**
   * JSONata unit test
   */
  @Test
  public void testLogic() throws Exception {
    testDriver("test.json");
  }

  /**
   * run the test spec in the given file
   */
  void testDriver(String file) throws Exception {
    ObjectMapper om =
        new ObjectMapper().configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true)
            .configure(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature(), true)
            .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature(), true);

    // read file and select expression
    JsonNode map = om.readTree(new FileInputStream(new File(file)));
    file = map.get("test").get("file").asText();
    JsonNode test = om.readTree(new FileInputStream(new File(file)));
    String expr =
        Expression.jsonata(map.get("test").get("expression").asText()).evaluate(test).asText();

    // iterate cases
    Iterator<String> cases = map.get("cases").fieldNames();
    while (cases.hasNext()) {
      String name = cases.next();
      ObjectNode c = (ObjectNode) map.get("cases").get(name);

      if (c.has("url")) {
        URL url = new URL(c.get("url").asText());
        if (c.get("url").asText().endsWith(".json"))
          c.set("data", objectMapper.readTree(url));
        else
          c.put("data", IOUtils.toString(url.openStream()));
      }

      if (map.has("basedata")) {
        ObjectNode basedata = map.get("basedata").deepCopy();

        // merge base data
        Iterator<String> dataFields = c.get("data").fieldNames();
        while (dataFields.hasNext()) {
          String field = dataFields.next();
          basedata.set(field, c.get("data").get(field));
        }

        // eval and check
        Assert.assertEquals("error in case: " + name, "" + c.get("expected"),
            "" + evaluate(expr, basedata));
      } else {
        // eval and check
        Assert.assertEquals("error in case: " + name, "" + c.get("expected"),
            "" + evaluate(expr, c.get("data")));
      }
    }
  }

  @SuppressWarnings("unused")
  JsonNode evaluate(String expr, JsonNode basedata) throws Exception {
    if (false)
      // uses local JSONata4J (not recommended since there are slight differences to JSONataJS)
      return Expression.jsonata(expr).evaluate(basedata);
    else {
      // send to http://admin:djdjdj@localhost:8080
      URL url = new URL("http://localhost:8080/rest/expression");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setRequestProperty("content-type", "application/json");
      connection.setRequestProperty("Authorization",
          "Basic " + Base64.getEncoder().encodeToString("admin:djdjdj".getBytes()));
      ObjectNode expAndData = objectMapper.createObjectNode();
      expAndData.set("data", basedata);
      expAndData.put("expression", expr);
      objectMapper.writeValue(new OutputStreamWriter(connection.getOutputStream()), expAndData);
      return objectMapper.readTree(connection.getInputStream());
    }
  }

  @SuppressWarnings("unused")
  void parse(String expression) throws Exception {
    if (false) {
      // uses local JSONata4J (not recommended since there are slight differences to JSONataJS)
      Expressions expr = Expressions.parse(expression);

      // register Dashjoin custom functions
      for (String function : functions)
        expr.getEnvironment().setJsonataFunction(function, new FloorFunction());
      try {
        // evaluate
        expr.evaluate(null);
      } catch (EvaluateException e) {
        // function signature does not match is expected
        Assert.assertTrue("Invalid expression: " + expression,
            e.getMessage().contains("does not match function signature"));
      }
    } else {
      // send to http://admin:djdjdj@localhost:8080
      URL url = new URL("http://localhost:8080/rest/expression-preview/parse");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setRequestProperty("content-type", "application/json");
      connection.setRequestProperty("Authorization",
          "Basic " + Base64.getEncoder().encodeToString("admin:djdjdj".getBytes()));
      try (OutputStream os = connection.getOutputStream()) {
        os.write(expression.getBytes());
      }
      objectMapper.readTree(connection.getInputStream());
    }
  }
}
