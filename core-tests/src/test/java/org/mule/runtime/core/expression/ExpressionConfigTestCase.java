/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.expression;

import static org.junit.Assert.assertEquals;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Before;
import org.junit.Test;

public class ExpressionConfigTestCase extends AbstractMuleContextTestCase {

  private DefaultExpressionManager expressionManager;

  @Before
  public void setup() throws InitialisationException {
    expressionManager = new DefaultExpressionManager();
    expressionManager.setMuleContext(muleContext);
    expressionManager.initialise();
  }

  @Test
  public void testExpressionLanguageExpression() throws Exception {
    ExpressionConfig config = new ExpressionConfig("message.inboundProperty['foo']=='bar'", "$[", "]");

    assertEquals("$[message.inboundProperty['foo']=='bar']", config.getFullExpression(expressionManager));

    config = new ExpressionConfig();
    config.parse("#[message.inboundAttachment['baz']]");

    assertEquals("message.inboundAttachment['baz']", config.getExpression());
  }

  @Test
  public void testELExpression() {
    ExpressionConfig expressionConfig = new ExpressionConfig();
    expressionConfig.setExpression("message.payload");
    assertEquals("message.payload", expressionConfig.getExpression());
    assertEquals("#[message.payload]", expressionConfig.getFullExpression(expressionManager));
  }

  @Test
  public void testELExpressionWithBrackets() {
    ExpressionConfig expressionConfig = new ExpressionConfig();
    expressionConfig.setExpression("#[message.payload]");
    assertEquals("message.payload", expressionConfig.getExpression());
    assertEquals("#[message.payload]", expressionConfig.getFullExpression(expressionManager));
  }

  @Test
  public void testELExpressionWithTenaryIf() {
    ExpressionConfig expressionConfig = new ExpressionConfig();
    expressionConfig.setExpression("1==1?true:false");
    assertEquals("1==1?true:false", expressionConfig.getExpression());
    assertEquals("#[1==1?true:false]", expressionConfig.getFullExpression(expressionManager));
  }

  @Test
  public void testELExpressionWithForeach() {
    ExpressionConfig expressionConfig = new ExpressionConfig();
    expressionConfig.setExpression("for(a:[1,2,3){'1'}");
    assertEquals("for(a:[1,2,3){'1'}", expressionConfig.getExpression());
    assertEquals("#[for(a:[1,2,3){'1'}]", expressionConfig.getFullExpression(expressionManager));
  }

  @Test
  public void testELExpressionWithColonInString() {
    ExpressionConfig expressionConfig = new ExpressionConfig();
    expressionConfig.setExpression("'This is a message : msg'");
    assertEquals("'This is a message : msg'", expressionConfig.getExpression());
    assertEquals("#['This is a message : msg']", expressionConfig.getFullExpression(expressionManager));
  }
}
