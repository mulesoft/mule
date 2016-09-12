/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.keygenerator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.NotSerializableException;
import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;

public class ExpressionKeyGeneratorTestCase extends AbstractMuleTestCase {

  private static final String KEY = "KEY";
  private static final String EXPRESSION = "muleExpression";

  private ExpressionMuleEventKeyGenerator keyGenerator;
  private InternalMessage message;
  private Event event;

  @Before
  public void setUp() throws Exception {
    expressionLanguage = mock(ExpressionLanguage.class);
    MuleContext context = mock(MuleContext.class);
    when(context.getExpressionLanguage()).thenReturn(expressionLanguage);

    message = mock(InternalMessage.class);

    event = mock(Event.class);
    when(event.getMessage()).thenReturn(message);

    keyGenerator = new ExpressionMuleEventKeyGenerator();
    keyGenerator.setExpression(EXPRESSION);
    keyGenerator.setMuleContext(context);
  }

  private ExpressionLanguage expressionLanguage;

  @Test
  public void testGeneratesSerializableKey() throws Exception {
    when(expressionLanguage.evaluate(EXPRESSION, event, null)).thenReturn(KEY);
    Serializable key = keyGenerator.generateKey(event);

    assertEquals(KEY, key);
  }

  @Test(expected = NotSerializableException.class)
  public void testThrowsExceptionOnNonSerializableKey() throws Exception {
    when(expressionLanguage.evaluate(EXPRESSION, event, null)).thenReturn(null);
    keyGenerator.generateKey(event);
  }
}
