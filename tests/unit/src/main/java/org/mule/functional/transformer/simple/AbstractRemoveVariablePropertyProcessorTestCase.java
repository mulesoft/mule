/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.transformer.simple;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.expression.ExpressionManager;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.event.mutator.AbstractRemoveVariablePropertyProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@SmallTest
public abstract class AbstractRemoveVariablePropertyProcessorTestCase extends AbstractMuleContextTestCase {

  public static final Charset ENCODING = US_ASCII;
  public static final String PLAIN_STRING_KEY = "someText";
  public static final String PLAIN_STRING_VALUE = "someValue";
  public static final String EXPRESSION = "#[string:someValue]";
  public static final String EXPRESSION_VALUE = "expressionValueResult";
  public static final String NULL_EXPRESSION = "#[string:someValueNull]";
  public static final String NULL_EXPRESSION_VALUE = null;

  private MuleMessage message;
  private MuleEvent event;
  private MuleSession mockSession = mock(MuleSession.class);
  private MuleContext mockMuleContext = mock(MuleContext.class);
  private ExpressionManager mockExpressionManager = mock(ExpressionManager.class);
  private AbstractRemoveVariablePropertyProcessor removeVariableProcessor;


  public AbstractRemoveVariablePropertyProcessorTestCase(AbstractRemoveVariablePropertyProcessor abstractAddVariableProcessor) {
    removeVariableProcessor = abstractAddVariableProcessor;
  }

  @Before
  public void setUpTest() throws Exception {
    when(mockMuleContext.getConfiguration()).thenReturn(mock(MuleConfiguration.class));
    when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
    when(mockExpressionManager.parse(anyString(), any(MuleEvent.class), any(FlowConstruct.class)))
        .thenAnswer(invocation -> invocation.getArguments()[0]);
    when(mockExpressionManager.evaluate(EXPRESSION, event, null)).thenReturn(EXPRESSION_VALUE);
    removeVariableProcessor.setMuleContext(mockMuleContext);

    message = MuleMessage.builder().payload("").build();
    Flow flow = getTestFlow();
    event = MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(message).flow(flow).session(mockSession)
        .build();
  }

  @Test
  public void testRemoveVariable() throws MuleException {
    removeVariableProcessor.setIdentifier(PLAIN_STRING_KEY);
    removeVariableProcessor.initialise();
    event = removeVariableProcessor.process(event);
    verifyRemoved(event, PLAIN_STRING_KEY);
  }

  @Test
  public void testRemoveVariableUsingExpression() throws MuleException {
    removeVariableProcessor.setIdentifier(EXPRESSION);
    removeVariableProcessor.initialise();
    event = removeVariableProcessor.process(event);
    verifyRemoved(event, EXPRESSION_VALUE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRemoveVariableNullKey() throws InitialisationException, TransformerException {
    removeVariableProcessor.setIdentifier(null);
  }

  @Test // Don't fail.
  public void testRemoveVariableExpressionKeyNullValue() throws MuleException {
    removeVariableProcessor.setIdentifier(NULL_EXPRESSION);
    removeVariableProcessor.initialise();
    event = removeVariableProcessor.process(event);
  }

  @Test
  @Ignore
  public void testRemoveVariableWithRegexExpression() throws MuleException {
    addMockedPropeerties(event, new HashSet<>(Arrays.asList("MULE_ID", "MULE_CORRELATION_ID", "SomeVar", "MULE_GROUP_ID")));

    removeVariableProcessor.setIdentifier("MULE_(.*)");
    removeVariableProcessor.initialise();
    event = removeVariableProcessor.process(event);

    verifyRemoved(event, "MULE_ID");
    verifyRemoved(event, "MULE_CORRELATION_ID");
    verifyRemoved(event, "MULE_GROUP_ID");
    verifyNotRemoved(event, "SomeVar");
  }

  protected abstract void addMockedPropeerties(MuleEvent event, HashSet properties);

  protected abstract void verifyRemoved(MuleEvent mockEvent, String key);

  protected abstract void verifyNotRemoved(MuleEvent mockEvent, String somevar);

}
