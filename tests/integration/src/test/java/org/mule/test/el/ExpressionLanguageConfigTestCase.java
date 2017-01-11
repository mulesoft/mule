/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.el;

import static org.junit.Assert.assertEquals;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.test.AbstractIntegrationTestCase;

import java.text.DateFormat;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

public class ExpressionLanguageConfigTestCase extends AbstractIntegrationTestCase {

  ExpressionManager el;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/el/expression-language-config.xml";
  }

  @Before
  public void setup() {
    el = muleContext.getExpressionManager();
  }

  @Test
  public void testExpressionLanguageImport() {
    assertEquals(Locale.class, evaluate("loc"));
  }

  @Test
  public void testExpressionLanguageImportNoName() {
    assertEquals(DateFormat.class, evaluate("DateFormat"));
  }

  @Test
  public void testExpressionLanguageAlias() {
    assertEquals(muleContext.getConfiguration().getId(), evaluate("appName"));
  }

  @Test
  public void testExpressionLanguageGlobalFunction() {
    // NOTE: This indirectly asserts that echo() function defined in config file rather than external
    // function definition file is being used (otherwise hiOTHER' would be returned

    assertEquals("hi", evaluate("echo('hi')"));
  }

  @Test
  public void testExpressionLanguageGlobalFunctionFromFile() {
    assertEquals("hi", evaluate("echo2('hi')"));
  }

  @Test
  public void testExpressionLanguageGlobalFunctionUsingStaticContext() {
    assertEquals("Hello " + muleContext.getConfiguration().getId() + "!", evaluate("hello()"));
  }

  @Test
  public void testExpressionLanguageGlobalFunctionUsingMessageContext() throws Exception {
    assertEquals("123appended", el.evaluate("mel:appendPayload()", eventBuilder().message(InternalMessage.of("123")).build(),
                                            getTestFlow(muleContext))
        .getValue());
  }

  @Test
  public void testExpressionLanguageGlobalFunctionUsingMessageContextAndImport() throws Exception {
    assertEquals("321", el.evaluate("mel:reversePayload()", eventBuilder().message(InternalMessage.of("123")).build(),
                                    getTestFlow(muleContext))
        .getValue());
  }

  @Test
  public void testExpressionLanguageExecuteElement() throws Exception {
    flowRunner("flow").withPayload("foo").run();
  }

  private Object evaluate(String expression) {
    return el.evaluate("mel:" + expression).getValue();
  }

}
