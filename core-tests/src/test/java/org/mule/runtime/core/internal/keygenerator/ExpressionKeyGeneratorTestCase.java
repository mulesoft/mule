/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.keygenerator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;

public class ExpressionKeyGeneratorTestCase extends AbstractMuleTestCase {

  private static final String RESOLVED_KEY = "KEY";
  private static final String SINGLE_EXPRESSION = "#[expression]";

  private ExpressionMuleEventKeyGenerator keyGenerator;
  private InternalMessage message;
  private MuleContext muleContext;
  private InternalEvent event;
  private ExtendedExpressionManager expressionManager;

  @Before
  public void setUp() throws Exception {
    expressionManager = mock(ExtendedExpressionManager.class);
    muleContext = mock(MuleContext.class);
    doReturn(expressionManager).when(muleContext).getExpressionManager();

    message = mock(InternalMessage.class);

    event = mock(InternalEvent.class);
    when(event.getMessage()).thenReturn(message);

    keyGenerator = new ExpressionMuleEventKeyGenerator();
  }

  @Test
  public void testGeneratesSerializableKey() throws Exception {
    keyGenerator.setExpression(SINGLE_EXPRESSION);
    keyGenerator.setMuleContext(muleContext);
    TypedValue<String> typedValue = new TypedValue<>(RESOLVED_KEY, STRING);
    when(expressionManager.evaluate(SINGLE_EXPRESSION, event)).thenReturn(typedValue);
    Serializable key = keyGenerator.generateKey(event);

    assertEquals(RESOLVED_KEY, key);
  }

  @Test
  public void resolvesCompositeExpression() throws Exception {
    keyGenerator.setExpression(SINGLE_EXPRESSION + SINGLE_EXPRESSION);
    keyGenerator.setMuleContext(muleContext);
    when(expressionManager.parse(SINGLE_EXPRESSION + SINGLE_EXPRESSION, event, null)).thenReturn(RESOLVED_KEY);

    Serializable key = keyGenerator.generateKey(event);
    assertThat(key, equalTo(RESOLVED_KEY));
  }

  @Test(expected = IllegalStateException.class)
  public void testThrowsExceptionOnNonSerializableKey() throws Exception {
    keyGenerator.setExpression(SINGLE_EXPRESSION);
    keyGenerator.setMuleContext(muleContext);
    when(expressionManager.evaluate(SINGLE_EXPRESSION, event)).thenReturn(new TypedValue(null, OBJECT));
    keyGenerator.generateKey(event);
  }
}
