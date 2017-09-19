/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

@SmallTest
public class TemplateParserTestCase extends AbstractMuleTestCase {

  @Test
  public void squareBracesParserDefaultConfiguration() {
    TemplateParser tp = TemplateParser.createSquareBracesStyleParser();
    assertNotNull(tp.getStyle());
    assertEquals("[", tp.getStyle().getPrefix());
    assertEquals("]", tp.getStyle().getSuffix());
  }

  @Test
  public void squareBracesParserShouldReplaceKnownTokens() {
    TemplateParser tp = TemplateParser.createSquareBracesStyleParser();

    Map<String, String> map = Collections.singletonMap("fromAddress", "ross.mason@symphonysoft.com");
    String template = "smtp://[fromAddress]";

    String result = tp.parse(map, template);
    assertEquals("smtp://ross.mason@symphonysoft.com", result);
  }

  @Test
  public void squareBracesParserShouldNotReplaceUnknownTokens() {
    TemplateParser tp = TemplateParser.createSquareBracesStyleParser();

    Map<String, String> map = Collections.emptyMap();
    String template = "smtp://[toAddress]";

    String result = tp.parse(map, template);
    assertEquals("smtp://[toAddress]", result);
  }

  @Test
  public void squareBracesParserShouldValidateExpressionDelimiters() {
    TemplateParser tp = TemplateParser.createSquareBracesStyleParser();
    assertTrue(tp.isValid("[1][2]"));
    assertFalse(tp.isValid("[[1]2]"));
    assertFalse(tp.isValid("[[1][2]"));
  }

  @Test
  public void antParserDefaultConfiguration() {
    TemplateParser tp = TemplateParser.createAntStyleParser();
    assertNotNull(tp.getStyle());
    assertEquals("${", tp.getStyle().getPrefix());
    assertEquals("}", tp.getStyle().getSuffix());
  }

  @Test
  public void antParserShouldValidateExpressionDelimiters() {
    TemplateParser tp = TemplateParser.createAntStyleParser();
    assertTrue(tp.isValid("${1}"));
    assertTrue(tp.isValid("${1}${2}"));
    assertTrue(tp.isValid("${1}&{}"));
    assertTrue(tp.isValid("{}${1}"));
    assertTrue(tp.isValid("${$}${1}"));
    assertFalse(tp.isValid("${${1}}${2}"));
    assertTrue(tp.isValid("$ {1}"));
  }

  @Test
  public void antParserShouldReplaceKnownTokens() {
    TemplateParser tp = TemplateParser.createAntStyleParser();

    Map<String, Object> map = buildMap();
    String template = "Some String with ${prop1} and ${prop2} in it";

    String result = tp.parse(map, template);
    assertEquals("Some String with value1 and value2 in it", result);
  }


  @Test
  public void antParserShouldNotReplaceUnknownTokens() {
    TemplateParser tp = TemplateParser.createAntStyleParser();

    Map<String, String> map = Collections.emptyMap();
    String template = "Some String with ${prop1} in it";

    String result = tp.parse(map, template);
    assertEquals("Some String with ${prop1} in it", result);
  }

  @Test
  public void antParserShouldHandleWhitespaceAndBackslashesCorrectly() {
    TemplateParser tp = TemplateParser.createAntStyleParser();

    String dir = "C:\\Documents and Settings\\";
    Map<String, String> map = Collections.singletonMap("dir", dir);
    String template = "start${dir}end";

    String result = tp.parse(map, template);
    assertEquals("startC:\\Documents and Settings\\end", result);
  }

  @Test
  public void antParserWithListInputShouldReplaceKnownTokens() {
    TemplateParser tp = TemplateParser.createAntStyleParser();

    Map<String, Object> map = buildMap();

    List<String> templates = new ArrayList<String>();
    templates.add("Some String with ${prop1} and ${prop2} in it");
    templates.add("Some String with ${prop1} in it");

    List<?> result = tp.parse(map, templates);
    assertEquals("Some String with value1 and value2 in it", result.get(0));
    assertEquals("Some String with value1 in it", result.get(1));
  }

  @Test
  public void antParserWithNullListInputShouldNotReplaceTokens() {
    TemplateParser tp = TemplateParser.createAntStyleParser();

    Map<String, Object> map = buildMap();
    List<?> result = tp.parse(map, (List<?>) null);
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  public void antParserWithSimilarTokensShouldNotBeConfused() {
    TemplateParser tp = TemplateParser.createAntStyleParser();

    Map<String, Object> map = buildMap();
    map.put("prop1-2", "value2");

    String template = "Some String with ${prop1} and ${prop1-2} in it";

    String result = tp.parse(map, template);
    assertEquals("Some String with value1 and value2 in it", result);
  }

  @Test
  public void antParserWithOptionalTokenShouldReplaceKnownTokens() {
    TemplateParser tp = TemplateParser.createAntStyleParser();

    Map<String, Object> props = new HashMap<String, Object>();
    props.put("prop1?", "value1");
    props.put("prop1-2", "value2");

    String template = "Some String with ${prop1?} and ${prop1-2} in it";

    String result = tp.parse(props, template);
    assertEquals("Some String with value1 and value2 in it", result);
  }

  @Test
  public void muleParserManagesPipeCharacter() {
    TemplateParser tp = TemplateParser.createMuleStyleParser();

    final String expectedResult = "Hello|Hi";

    String result = tp.parse(null, "#[mel:evaluator: 'Hello|Hi']", new TemplateParser.TemplateCallback() {

      public Object match(String token) {

        return expectedResult;
      }
    });

    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserManagesNestedSquareBrackets() {
    TemplateParser tp = TemplateParser.createMuleStyleParser();
    final String expectedResult = "zero[one[two[three[four[five]]]]]";
    String expression = "#[zero[one[two[three[four[five]]]]]]";
    assertTrue(tp.isValid(expression));
    String result = tp.parse(null, expression, new TemplateParser.TemplateCallback() {

      @Override
      public Object match(String token) {
        return token;
      }
    });
    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserManagesNestedExpressions() {
    TemplateParser tp = TemplateParser.createMuleStyleParser();
    final String expectedResult = "mel:zero#[mel:one#[mel:two#[mel:three#[mel:four#[mel:five]]]]]";
    String expression = "#[mel:zero#[mel:one#[mel:two#[mel:three#[mel:four#[mel:five]]]]]]";
    assertTrue(tp.isValid(expression));
    String result = tp.parse(null, expression, new TemplateParser.TemplateCallback() {

      @Override
      public Object match(String token) {
        return token;
      }
    });
    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserManagesConcatenation() {
    TemplateParser tp = TemplateParser.createMuleStyleParser();

    final String expectedResult = "'hi'+'world'";

    String result = tp.parse(null, "#['hi'+'world']", new TemplateParser.TemplateCallback() {

      public Object match(String token) {

        return token;
      }
    });

    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserManagesNullExpressions() {
    TemplateParser tp = TemplateParser.createMuleStyleParser();

    final String expectedResult = "null";

    String result = tp.parse(null, "#[mel:expression that returns null]", new TemplateParser.TemplateCallback() {

      public Object match(String token) {
        return null;
      }
    });

    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserDefaultConfiguration() {
    TemplateParser tp = TemplateParser.createMuleStyleParser();
    assertNotNull(tp.getStyle());
    assertEquals("#[", tp.getStyle().getPrefix());
    assertEquals("]", tp.getStyle().getSuffix());
  }

  @Test
  public void muleParserShouldValidateExpressionDelimiters() {
    TemplateParser tp = TemplateParser.createMuleStyleParser();

    assertTrue(tp.isValid("#[mel:]"));
    assertTrue(tp.isValid("#[mel:]   #[mel:]"));
    assertTrue(tp.isValid("#[mel:]&[]"));
    assertTrue(tp.isValid("[]$[]#"));
    assertTrue(tp.isValid("#[mel:#]#[mel:]"));
    assertTrue(tp.isValid("#[mel:#[mel:]]#[mel:]"));
    assertTrue(tp.isValid("# []"));
    assertTrue(tp.isValid("#[mel:one[]two[]three[]four[]five[]six[]seven[]eight[]]"));

    // can't have unbalanced brackets
    assertFalse(tp.isValid("#[mel:#[mel:]#[mel:]"));
    assertFalse(tp.isValid("#[mel:[][]"));

    assertTrue(tp.isValid("#[mel:foo:blah[4] = 'foo']"));
    assertTrue(tp.isValid("#[mel:foo:blah[4] = '#foo']"));
    assertTrue(tp.isValid("#[mel:foo:blah4] = '#foo']"));
    assertTrue(tp.isValid("#[mel:foo:blah = '#[mel:foo]']"));
  }

  private Map<String, Object> buildMap() {
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("prop1", "value1");
    props.put("prop2", "value2");
    return props;
  }
}
