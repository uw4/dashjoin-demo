package org.dashjoin.app;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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
      "page", "tree", "variable", "sidenav-switch", /* json schema form */ "textarea", "select");

  /**
   * dashjoin JSONata functions
   */
  List<String> functions = Arrays.asList("$all", "$read", "$create", "$update", "$traverse",
      "$delete", "$query", "$queryGraph", "$call", "$incoming", "$trigger", "$djRoles",
      "$djVersion", "$djGetDatabases", "$djGetDrivers", "$djGetFunctions", "$echo",
      "$alterColumnTrigger", "$alterTableTrigger", "$doc2data", "$djSubscription", "$crawl");

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
            Assert.assertTrue(allowedWidget.contains(widget.asText()));
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
              Expressions expr = Expressions.parse(expression.asText());

              // register Dashjoin custom functions
              for (String function : functions)
                expr.getEnvironment().setJsonataFunction(function, new FloorFunction());
              try {
                // evaluate
                expr.evaluate(null);
              } catch (EvaluateException e) {
                // function signature does not match is expected
                Assert.assertTrue(e.getMessage().contains("does not match function signature"));
              }
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
    Expression e = Expression.jsonata(expr);
    Iterator<String> cases = map.get("cases").fieldNames();
    while (cases.hasNext()) {
      String name = cases.next();
      JsonNode c = map.get("cases").get(name);
      ObjectNode basedata = map.get("basedata").deepCopy();

      // merge base data
      Iterator<String> dataFields = c.get("data").fieldNames();
      while (dataFields.hasNext()) {
        String field = dataFields.next();
        basedata.set(field, c.get("data").get(field));
      }

      // eval and check
      Assert.assertEquals("error in case: " + name, "" + c.get("expected"),
          "" + e.evaluate(basedata));
    }
  }
}
