/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.keygenerator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.NotSerializableException;
import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;

public class ExpressionKeyGeneratorTestCase extends AbstractMuleTestCase {

  private static final String RESOLVED_KEY = "KEY";
  private static final String SINGLE_EXPRESSION = "#[expression]";

  private ExpressionMuleEventKeyGenerator keyGenerator;
  private InternalMessage message;
  private MuleContext muleContext;
  private Event event;

  @Before
  public void setUp() throws Exception {
    keyGenerator = new ExpressionMuleEventKeyGenerator();
    expressionLanguage = mock(ExpressionLanguage.class);
    muleContext = mock(MuleContext.class);
    when(muleContext.getExpressionLanguage()).thenReturn(expressionLanguage);

    message = mock(InternalMessage.class);

    event = mock(Event.class);
    when(event.getMessage()).thenReturn(message);
  }

  private ExpressionLanguage expressionLanguage;

  @Test
  public void testGeneratesSerializableKey() throws Exception {
    keyGenerator.setExpression(SINGLE_EXPRESSION);
    keyGenerator.setMuleContext(muleContext);
    when(expressionLanguage.evaluate(SINGLE_EXPRESSION, event, null)).thenReturn(RESOLVED_KEY);
    Serializable key = keyGenerator.generateKey(event);

    assertEquals(RESOLVED_KEY, key);
  }

  @Test
  public void resolvesCompositeExpression() throws Exception {
    keyGenerator.setExpression(SINGLE_EXPRESSION + SINGLE_EXPRESSION);
    keyGenerator.setMuleContext(muleContext);
    when(expressionLanguage.parse(SINGLE_EXPRESSION + SINGLE_EXPRESSION, event, null)).thenReturn(RESOLVED_KEY);

    Serializable key = keyGenerator.generateKey(event);
    assertThat(key, equalTo(RESOLVED_KEY));
  }

  @Test(expected = NotSerializableException.class)
  public void testThrowsExceptionOnNonSerializableKey() throws Exception {
    keyGenerator.setExpression(SINGLE_EXPRESSION);
    keyGenerator.setMuleContext(muleContext);
    when(expressionLanguage.evaluate(SINGLE_EXPRESSION, event, null)).thenReturn(null);

    keyGenerator.generateKey(event);
  }
}
