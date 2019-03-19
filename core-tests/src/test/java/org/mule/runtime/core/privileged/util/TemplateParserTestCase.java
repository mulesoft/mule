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
import static org.mule.runtime.core.privileged.util.TemplateParser.createAntStyleParser;
import static org.mule.runtime.core.privileged.util.TemplateParser.createMuleStyleParser;
import static org.mule.runtime.core.privileged.util.TemplateParser.createSquareBracesStyleParser;

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
    TemplateParser tp = createSquareBracesStyleParser();
    assertNotNull(tp.getStyle());
    assertEquals("[", tp.getStyle().getPrefix());
    assertEquals("]", tp.getStyle().getSuffix());
  }

  @Test
  public void squareBracesParserShouldReplaceKnownTokens() {
    TemplateParser tp = createSquareBracesStyleParser();

    Map<String, String> map = Collections.singletonMap("fromAddress", "ross.mason@symphonysoft.com");
    String template = "smtp://[fromAddress]";

    String result = tp.parse(map, template);
    assertEquals("smtp://ross.mason@symphonysoft.com", result);
  }

  @Test
  public void squareBracesParserShouldNotReplaceUnknownTokens() {
    TemplateParser tp = createSquareBracesStyleParser();

    Map<String, String> map = Collections.emptyMap();
    String template = "smtp://[toAddress]";

    String result = tp.parse(map, template);
    assertEquals("smtp://[toAddress]", result);
  }

  @Test
  public void squareBracesParserShouldValidateExpressionDelimiters() {
    TemplateParser tp = createSquareBracesStyleParser();
    assertTrue(tp.isValid("[1][2]"));
    assertFalse(tp.isValid("[[1]2]"));
    assertFalse(tp.isValid("[[1][2]"));
  }

  @Test
  public void antParserDefaultConfiguration() {
    TemplateParser tp = createAntStyleParser();
    assertNotNull(tp.getStyle());
    assertEquals("${", tp.getStyle().getPrefix());
    assertEquals("}", tp.getStyle().getSuffix());
  }

  @Test
  public void antParserShouldValidateExpressionDelimiters() {
    TemplateParser tp = createAntStyleParser();
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
    TemplateParser tp = createAntStyleParser();

    Map<String, Object> map = buildMap();
    String template = "Some String with ${prop1} and ${prop2} in it";

    String result = tp.parse(map, template);
    assertEquals("Some String with value1 and value2 in it", result);
  }


  @Test
  public void antParserShouldNotReplaceUnknownTokens() {
    TemplateParser tp = createAntStyleParser();

    Map<String, String> map = Collections.emptyMap();
    String template = "Some String with ${prop1} in it";

    String result = tp.parse(map, template);
    assertEquals("Some String with ${prop1} in it", result);
  }

  @Test
  public void antParserShouldHandleWhitespaceAndBackslashesCorrectly() {
    TemplateParser tp = createAntStyleParser();

    String dir = "C:\\Documents and Settings\\";
    Map<String, String> map = Collections.singletonMap("dir", dir);
    String template = "start${dir}end";

    String result = tp.parse(map, template);
    assertEquals("startC:\\Documents and Settings\\end", result);
  }

  @Test
  public void antParserWithListInputShouldReplaceKnownTokens() {
    TemplateParser tp = createAntStyleParser();

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
    TemplateParser tp = createAntStyleParser();

    Map<String, Object> map = buildMap();
    List<?> result = tp.parse(map, (List<?>) null);
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  public void antParserWithSimilarTokensShouldNotBeConfused() {
    TemplateParser tp = createAntStyleParser();

    Map<String, Object> map = buildMap();
    map.put("prop1-2", "value2");

    String template = "Some String with ${prop1} and ${prop1-2} in it";

    String result = tp.parse(map, template);
    assertEquals("Some String with value1 and value2 in it", result);
  }

  @Test
  public void antParserWithOptionalTokenShouldReplaceKnownTokens() {
    TemplateParser tp = createAntStyleParser();

    Map<String, Object> props = new HashMap<String, Object>();
    props.put("prop1?", "value1");
    props.put("prop1-2", "value2");

    String template = "Some String with ${prop1?} and ${prop1-2} in it";

    String result = tp.parse(props, template);
    assertEquals("Some String with value1 and value2 in it", result);
  }

  @Test
  public void muleParserManagesPipeCharacter() {
    TemplateParser tp = createMuleStyleParser();

    final String expectedResult = "mel:evaluator: 'Hello|Hi'";

    String result = tp.parse(null, "#[mel:evaluator: 'Hello|Hi']", token -> token);

    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserManagesNestedSquareBrackets() {
    TemplateParser tp = createMuleStyleParser();
    final String expectedResult = "zero[one[two[three[four[five]]]]]";
    String expression = "#[zero[one[two[three[four[five]]]]]]";
    assertTrue(tp.isValid(expression));
    String result = tp.parse(null, expression, token -> token);
    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserManagesNestedExpressions() {
    TemplateParser tp = createMuleStyleParser();
    final String expectedResult = "mel:zero mel:one mel:two mel:three mel:four mel:five";
    String expression = "#[mel:zero #[mel:one #[mel:two #[mel:three #[mel:four #[mel:five]]]]]]";
    assertTrue(tp.isValid(expression));
    String result = tp.parse(null, expression, token -> token);
    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserManagesConcatenation() {
    TemplateParser tp = createMuleStyleParser();

    final String expectedResult = "'hi'+'world'";

    String result = tp.parse(null, "#['hi'+'world']", token -> token);

    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserManagesNullExpressions() {
    TemplateParser tp = createMuleStyleParser();

    final String expectedResult = "null";

    String result = tp.parse(null, "#[mel:expression that returns null]", token -> null);

    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserDefaultConfiguration() {
    TemplateParser tp = createMuleStyleParser();
    assertNotNull(tp.getStyle());
    assertEquals("#[", tp.getStyle().getPrefix());
    assertEquals("]", tp.getStyle().getSuffix());
  }

  @Test
  public void muleParserShouldValidateExpressionDelimiters() {
    TemplateParser tp = createMuleStyleParser();

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
    assertTrue(tp.isValid("#[mel:[][]]"));

    assertTrue(tp.isValid("#[mel:foo:blah[4] = 'foo']"));
    assertTrue(tp.isValid("#[mel:foo:blah[4] = '#foo']"));
    assertTrue(tp.isValid("#[mel:foo:blah4] = '#foo']"));
    assertTrue(tp.isValid("#[mel:foo:blah = '#[mel:foo]']"));
  }

  @Test
  public void testWithoutSharp() {
    TemplateParser tp = createMuleStyleParser();
    assertTrue(tp.isValid(""));
    assertTrue(tp.isValid("]]]]]"));
    assertTrue(tp.isValid("["));
    assertTrue(tp.isValid("[]"));
    assertTrue(tp.isValid("[]]]]]"));
    assertTrue(tp.isValid("[][][][][[]]"));
    assertTrue(tp.isValid("\""));
    assertTrue(tp.isValid("\\'"));
    assertTrue(tp.isValid("'"));
    assertTrue(tp.isValid("''"));
    assertTrue(tp.isValid("\"\""));
    assertTrue(tp.isValid("\"'"));
    assertTrue(tp.isValid("'\""));
    assertTrue(tp.isValid("'\"'"));
    assertTrue(tp.isValid("'\""));
    assertTrue(tp.isValid("'\"\""));
    assertTrue(tp.isValid("'\"\"'"));
    assertTrue(tp.isValid("\"'\"'"));
    assertTrue(tp.isValid("'\"'\""));
    assertTrue(tp.isValid("'\"'\"'"));
    assertTrue(tp.isValid("'\"\"''"));
    assertTrue(tp.isValid("   '\"''\"'  "));
  }

  @Test
  public void testWithSharp() {
    TemplateParser tp = createMuleStyleParser();
    assertTrue(tp.isValid("#"));
    assertTrue(tp.isValid("##[]"));
    assertTrue(tp.isValid("#[]"));
    assertTrue(tp.isValid("#[[]]"));
    assertTrue(tp.isValid("#['[']"));
    assertTrue(tp.isValid("#[']']"));
    assertTrue(tp.isValid("#[#[]]"));
    assertFalse(tp.isValid("#[[]"));
    assertTrue(tp.isValid("#[#[#[[]]]]"));
    assertFalse(tp.isValid("#[#[#[[]]]"));
    assertTrue(tp.isValid("#[#[#[[]]]]["));
    assertFalse(tp.isValid("#[#[#[[]]]]#["));
    assertTrue(tp.isValid("#[#[#[[]]]]#[]"));
    assertTrue(tp.isValid("#[#[#['[]']]]#[]"));
    assertTrue(tp.isValid("#[#[#['[]']]]\\#["));
    assertTrue(tp.isValid("#[#[#['[']]]#[]"));
    assertTrue(tp.isValid("#['[']]"));
    assertTrue(tp.isValid("asjdhkasdhaskldh #[asdadasd[asdadasd]asdadsadasd'['dfad] asdasdasd"));
    assertTrue(tp.isValid("asjdhkasdhaskldh #['asdadasd[asdadasdasdadsadasd]asdasdasd#[']"));
    assertTrue(tp.isValid("#[]#[]"));
    assertTrue(tp.isValid("#[[]]"));
    assertTrue(tp.isValid("\\#["));
    assertTrue(tp.isValid("\\#['"));
    assertTrue(tp.isValid("#['']"));
    assertFalse(tp.isValid("#[']"));
    assertFalse(tp.isValid("#[']'"));
  }

  @Test
  public void muleParserWithStringBracesInside() {
    TemplateParser tp = createMuleStyleParser();
    final String expectedResult = "'['";
    String expression = "#['[']";
    assertTrue(tp.isValid(expression));
    String result = tp.parse(null, expression, token -> token);
    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserWithScapedQuote() {
    TemplateParser tp = createMuleStyleParser();
    final String expectedResult = "\"";
    String expression = "#[\\\"]";
    assertTrue(tp.isValid(expression));
    String result = tp.parse(null, expression, token -> token);
    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserWithBackSlashWithoutQuote() {
    TemplateParser tp = createMuleStyleParser();
    final String expectedResult = "\\a";
    String expression = "#[\\a]";
    assertTrue(tp.isValid(expression));
    String result = tp.parse(null, expression, token -> token);
    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserWithExpresionInside() {
    TemplateParser tp = createMuleStyleParser();
    final String expression = "#[hello #[mule]]";
    String expectedResult = "hello mule";
    assertTrue(tp.isValid(expression));

    String result = tp.parse(null, expression, token -> token);
    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserWithValueWithSharps() {
    TemplateParser tp = createMuleStyleParser();
    final String expression = "#[hello mule]";
    String expectedResult = "sarasa # sarasa2";
    assertTrue(tp.isValid(expression));

    String result = tp.parse(null, expression, token -> "sarasa # sarasa2");
    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserWithValueWithScapedSharps() {
    TemplateParser tp = createMuleStyleParser();
    final String expression = "#[hello mule]";
    String expectedResult = "sarasa \\# sarasa2";
    assertTrue(tp.isValid(expression));

    String result = tp.parse(null, expression, token -> "sarasa \\\\# sarasa2");
    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserWithExpresionInsideWithoutSharp() {
    TemplateParser tp = createMuleStyleParser();
    final String expression = "#[hello [mule]]";
    String expectedResult = "hello [mule]";
    assertTrue(tp.isValid(expression));
    String result = tp.parse(null, expression, token -> token);
    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserWithExpresionInsideWithoutLiteralWithoutSharp() {
    TemplateParser tp = createMuleStyleParser();
    final String expression = "#[[mule]]";
    String expectedResult = "[mule]";
    assertTrue(tp.isValid(expression));
    String result = tp.parse(null, expression, token -> token);
    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserWithExpresionInsideWithoutLiteral() {
    TemplateParser tp = createMuleStyleParser();
    final String expression = "#[#[mule]]";
    String expectedResult = "mule";
    assertTrue(tp.isValid(expression));
    String result = tp.parse(null, expression, token -> token);
    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParserWithLiteralBefore() {
    TemplateParser tp = createMuleStyleParser();
    final String expectedResult = "muleman 'value'";
    String expression = "muleman #['value']";
    assertTrue(tp.isValid(expression));
    String result = tp.parse(null, expression, token -> token);
    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParseConsecutiveExpressions() {
    TemplateParser tp = createMuleStyleParser();
    final String expectedResult = "muleman value";
    String expression = "#[muleman] #[value]";
    assertTrue(tp.isValid(expression));
    String result = tp.parse(null, expression, token -> token);
    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParseConsecutiveSharps() {
    TemplateParser tp = createMuleStyleParser();
    final String expectedResult = "#muleman # ##value";
    String expression = "##[muleman] # ###[value]";
    assertTrue(tp.isValid(expression));
    String result = tp.parse(null, expression, token -> token);
    assertEquals(expectedResult, result);
  }

  @Test
  public void muleParseMultiLine() {
    TemplateParser tp = createMuleStyleParser();
    final String expectedResult = "{\n" +
        "    \"text\" : \"#\n" +
        "    3\"\n" +
        "}";
    String expression = "#[{\n" +
        "    \"text\" : \"#\n" +
        "    3\"\n" +
        "}]";
    assertTrue(tp.isValid(expression));
    String result = tp.parse(null, expression, token -> token);
    assertEquals(expectedResult, result);
  }

  private Map<String, Object> buildMap() {
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("prop1", "value1");
    props.put("prop2", "value2");
    return props;
  }
}
