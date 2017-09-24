/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.transformer.simple;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.privileged.processor.simple.AbstractRemoveVariablePropertyProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

@SmallTest
public abstract class AbstractRemoveVariablePropertyProcessorTestCase extends AbstractMuleContextTestCase {

  public static final Charset ENCODING = US_ASCII;
  public static final String PLAIN_STRING_KEY = "someText";
  public static final String PLAIN_STRING_VALUE = "someValue";
  public static final String EXPRESSION = "#[string:someValue]";
  public static final String EXPRESSION_VALUE = "expressionValueResult";
  public static final String NULL_EXPRESSION = "#[string:someValueNull]";
  public static final String NULL_EXPRESSION_VALUE = null;

  private Message message;
  private CoreEvent event;
  private MuleContext mockMuleContext = mock(MuleContext.class);
  private ExtendedExpressionManager mockExpressionManager = mock(ExtendedExpressionManager.class);
  private TypedValue<String> typedValue;
  private AbstractRemoveVariablePropertyProcessor removeVariableProcessor;

  public AbstractRemoveVariablePropertyProcessorTestCase(AbstractRemoveVariablePropertyProcessor abstractAddVariableProcessor) {
    removeVariableProcessor = abstractAddVariableProcessor;
  }

  @Before
  public void setUpTest() throws Exception {
    message = of("");
    event = createTestEvent(message);

    when(mockMuleContext.getConfiguration()).thenReturn(mock(MuleConfiguration.class));
    when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
    typedValue = new TypedValue<>(EXPRESSION_VALUE, STRING);
    when(mockExpressionManager.evaluate(eq(EXPRESSION), eq(STRING), any(), eq(event))).thenReturn(typedValue);
    removeVariableProcessor.setMuleContext(mockMuleContext);
  }

  protected CoreEvent createTestEvent(final Message message) throws MuleException {
    return eventBuilder(muleContext).message(message).build();
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
    TypedValue typedValue = new TypedValue(null, OBJECT);
    when(mockExpressionManager.evaluate(eq(NULL_EXPRESSION), eq(DataType.STRING), any(), eq(event))).thenReturn(typedValue);
    removeVariableProcessor.setIdentifier(NULL_EXPRESSION);
    removeVariableProcessor.initialise();
    event = removeVariableProcessor.process(event);
  }

  @Test
  @Ignore
  public void testRemoveVariableWithRegexExpression() throws MuleException {
    addMockedPropeerties(event, new HashSet<>(asList("MULE_ID", "MULE_CORRELATION_ID", "SomeVar", "MULE_GROUP_ID")));

    removeVariableProcessor.setIdentifier("MULE_(.*)");
    removeVariableProcessor.initialise();
    event = removeVariableProcessor.process(event);

    verifyRemoved(event, "MULE_ID");
    verifyRemoved(event, "MULE_CORRELATION_ID");
    verifyRemoved(event, "MULE_GROUP_ID");
    verifyNotRemoved(event, "SomeVar");
  }

  protected abstract void addMockedPropeerties(CoreEvent event, Set<String> properties);

  protected abstract void verifyRemoved(CoreEvent mockEvent, String key);

  protected abstract void verifyNotRemoved(CoreEvent mockEvent, String somevar);

}
