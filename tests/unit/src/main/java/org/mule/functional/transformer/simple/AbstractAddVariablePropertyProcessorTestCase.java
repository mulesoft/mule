/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.transformer.simple;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
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
import org.mule.runtime.core.event.mutator.AbstractAddVariablePropertyProcessor;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.nio.charset.Charset;

import javax.activation.MimeTypeParseException;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public abstract class AbstractAddVariablePropertyProcessorTestCase extends AbstractMuleContextTestCase {

  public static final Charset ENCODING = US_ASCII;
  public static final String PLAIN_STRING_KEY = "someText";
  public static final String PLAIN_STRING_VALUE = "someValue";
  public static final String EXPRESSION = "#[string:someValue]";
  public static final String EXPRESSION_VALUE = "expressionValueResult";
  public static final String NULL_EXPRESSION = "#[string:someValueNull]";
  public static final Charset CUSTOM_ENCODING = UTF_8;

  private MuleEvent event;
  private MuleMessage message;
  private MuleSession mockSession = mock(MuleSession.class);
  private MuleContext mockMuleContext = mock(MuleContext.class);
  private ExpressionManager mockExpressionManager = mock(ExpressionManager.class);
  private AbstractAddVariablePropertyProcessor addVariableProcessor;

  public AbstractAddVariablePropertyProcessorTestCase(AbstractAddVariablePropertyProcessor abstractAddVariableProcessor) {
    addVariableProcessor = abstractAddVariableProcessor;
  }

  @Before
  public void setUpTest() throws Exception {
    when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
    when(mockMuleContext.getConfiguration()).thenReturn(mock(MuleConfiguration.class));
    when(mockExpressionManager.parse(anyString(), any(MuleEvent.class), any(FlowConstruct.class)))
        .thenAnswer(invocation -> invocation.getArguments()[0]);
    when(mockExpressionManager.evaluate(eq(EXPRESSION), any(MuleEvent.class), any(FlowConstruct.class)))
        .thenReturn(EXPRESSION_VALUE);
    TypedValue typedValue = new TypedValue(EXPRESSION_VALUE, DataType.STRING);
    when(mockExpressionManager.evaluateTyped(eq(EXPRESSION), any(MuleEvent.class), any(FlowConstruct.class)))
        .thenReturn(typedValue);
    addVariableProcessor.setMuleContext(mockMuleContext);

    message = MuleMessage.builder().payload("").build();
    Flow flow = getTestFlow();
    event = MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(message).flow(flow).session(mockSession)
        .build();
  }

  @Test
  public void testAddVariable() throws MuleException {
    addVariableProcessor.setIdentifier(PLAIN_STRING_KEY);
    addVariableProcessor.setValue(PLAIN_STRING_VALUE);
    addVariableProcessor.initialise();
    event = addVariableProcessor.process(event);

    verifyAdded(event, PLAIN_STRING_KEY, PLAIN_STRING_VALUE);
    assertThat(getVariableDataType(event, PLAIN_STRING_KEY),
               like(String.class, MediaType.ANY, getDefaultEncoding(mockMuleContext)));
  }

  @Test
  public void testAddVariableWithExpressionValue() throws MuleException {
    addVariableProcessor.setIdentifier(PLAIN_STRING_KEY);
    addVariableProcessor.setValue(EXPRESSION);
    addVariableProcessor.initialise();
    event = addVariableProcessor.process(event);

    verifyAdded(event, PLAIN_STRING_KEY, EXPRESSION_VALUE);
    assertThat(getVariableDataType(event, PLAIN_STRING_KEY),
               like(String.class, MediaType.ANY, getDefaultEncoding(mockMuleContext)));
  }

  @Test
  public void testAddVariableWithExpressionKey() throws MuleException {
    addVariableProcessor.setIdentifier(EXPRESSION);
    addVariableProcessor.setValue(PLAIN_STRING_VALUE);
    addVariableProcessor.initialise();
    event = addVariableProcessor.process(event);

    verifyAdded(event, EXPRESSION_VALUE, PLAIN_STRING_VALUE);
    assertThat(getVariableDataType(event, EXPRESSION_VALUE),
               like(String.class, MediaType.ANY, getDefaultEncoding(mockMuleContext)));
  }

  @Test
  public void testAddVariableWithEncoding() throws MuleException {
    addVariableProcessor.setIdentifier(PLAIN_STRING_KEY);
    addVariableProcessor.setValue(PLAIN_STRING_VALUE);
    addVariableProcessor.initialise();
    addVariableProcessor.setReturnDataType(DataType.builder().charset(CUSTOM_ENCODING).build());
    event = addVariableProcessor.process(event);

    verifyAdded(event, PLAIN_STRING_KEY, PLAIN_STRING_VALUE);
    assertThat(getVariableDataType(event, PLAIN_STRING_KEY), like(String.class, MediaType.ANY, CUSTOM_ENCODING));
  }

  @Test
  public void testAddVariableWithMimeType() throws MimeTypeParseException, MuleException {
    addVariableProcessor.setIdentifier(PLAIN_STRING_KEY);
    addVariableProcessor.setValue(PLAIN_STRING_VALUE);
    addVariableProcessor.initialise();
    addVariableProcessor.setReturnDataType(DataType.builder().mediaType(APPLICATION_XML).build());
    event = addVariableProcessor.process(event);

    verifyAdded(event, PLAIN_STRING_KEY, PLAIN_STRING_VALUE);
    assertThat(getVariableDataType(event, PLAIN_STRING_KEY),
               like(String.class, APPLICATION_XML, getDefaultEncoding(mockMuleContext)));
  }

  protected abstract DataType getVariableDataType(MuleEvent event, String key);

  @Test(expected = IllegalArgumentException.class)
  public void testAddVariableWithNullKey() throws InitialisationException, TransformerException {
    addVariableProcessor.setIdentifier(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddVariableWithEmptyKey() throws InitialisationException, TransformerException {
    addVariableProcessor.setIdentifier("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddVariableWithNullValue() throws InitialisationException, TransformerException {
    addVariableProcessor.setValue(null);
  }

  @Test
  public void testAddVariableWithNullExpressionKeyResult() throws MuleException {
    addVariableProcessor.setIdentifier(NULL_EXPRESSION);
    addVariableProcessor.setValue(PLAIN_STRING_VALUE);
    addVariableProcessor.initialise();
    event = addVariableProcessor.process(event);
    verifyNotAdded(event);
  }

  @Test
  public void testAddVariableWithNullExpressionValueResult() throws MuleException {
    addVariableProcessor.setIdentifier(PLAIN_STRING_KEY);
    TypedValue typedValue = new TypedValue(null, DataType.OBJECT);
    when(mockExpressionManager.evaluateTyped(NULL_EXPRESSION, event, null)).thenReturn(typedValue);
    addVariableProcessor.setValue(NULL_EXPRESSION);
    addVariableProcessor.initialise();
    event = addVariableProcessor.process(event);
    verifyRemoved(event, PLAIN_STRING_KEY);
  }

  @Test
  public void testAddVariableWithNullPayloadExpressionValueResult() throws MuleException {
    addVariableProcessor.setIdentifier(PLAIN_STRING_KEY);
    addVariableProcessor.setValue(EXPRESSION);
    TypedValue typedValue = new TypedValue(null, DataType.OBJECT);
    when(mockExpressionManager.evaluateTyped(EXPRESSION, event, null)).thenReturn(typedValue);
    addVariableProcessor.initialise();

    event = addVariableProcessor.process(event);

    verifyRemoved(event, PLAIN_STRING_KEY);
  }

  protected abstract void verifyAdded(MuleEvent event, String key, String value);

  protected abstract void verifyNotAdded(MuleEvent mockEvent);

  protected abstract void verifyRemoved(MuleEvent mockEvent, String key);

}
