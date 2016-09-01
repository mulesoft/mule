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
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.expression.ExpressionManager;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.transformer.simple.AbstractAddVariablePropertyTransformer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.nio.charset.Charset;

import javax.activation.MimeTypeParseException;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public abstract class AbstractAddVariablePropertyTransformerTestCase extends AbstractMuleContextTestCase {

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
  private AbstractAddVariablePropertyTransformer addVariableTransformer;

  public AbstractAddVariablePropertyTransformerTestCase(AbstractAddVariablePropertyTransformer abstractAddVariableTransformer) {
    addVariableTransformer = abstractAddVariableTransformer;
  }

  @Before
  public void setUpTest() throws Exception {
    addVariableTransformer.setReturnDataType(DataType.OBJECT);

    when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
    when(mockMuleContext.getConfiguration()).thenReturn(mock(MuleConfiguration.class));
    when(mockExpressionManager.parse(anyString(), any(MuleEvent.class), any(FlowConstruct.class)))
        .thenAnswer(invocation -> invocation.getArguments()[0]);
    when(mockExpressionManager.evaluate(eq(EXPRESSION), any(MuleEvent.class), any(FlowConstruct.class)))
        .thenReturn(EXPRESSION_VALUE);
    TypedValue typedValue = new TypedValue(EXPRESSION_VALUE, DataType.STRING);
    when(mockExpressionManager.evaluateTyped(eq(EXPRESSION), any(MuleEvent.class), any(FlowConstruct.class)))
        .thenReturn(typedValue);
    addVariableTransformer.setMuleContext(mockMuleContext);

    message = MuleMessage.builder().payload("").build();
    Flow flow = getTestFlow();
    event = MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(message).flow(flow).session(mockSession)
        .build();
  }

  @Test
  public void testAddVariable() throws InitialisationException, TransformerException {
    addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
    addVariableTransformer.setValue(PLAIN_STRING_VALUE);
    addVariableTransformer.initialise();
    addVariableTransformer.transform(event, ENCODING);

    verifyAdded(event, PLAIN_STRING_KEY, PLAIN_STRING_VALUE);
    assertThat(getVariableDataType(event, PLAIN_STRING_KEY),
               like(String.class, MediaType.ANY, getDefaultEncoding(mockMuleContext)));
  }

  @Test
  public void testAddVariableWithExpressionValue() throws InitialisationException, TransformerException {
    addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
    addVariableTransformer.setValue(EXPRESSION);
    addVariableTransformer.initialise();
    addVariableTransformer.transform(event, ENCODING);

    verifyAdded(event, PLAIN_STRING_KEY, EXPRESSION_VALUE);
    assertThat(getVariableDataType(event, PLAIN_STRING_KEY),
               like(String.class, MediaType.ANY, getDefaultEncoding(mockMuleContext)));
  }

  @Test
  public void testAddVariableWithExpressionKey() throws InitialisationException, TransformerException {
    addVariableTransformer.setIdentifier(EXPRESSION);
    addVariableTransformer.setValue(PLAIN_STRING_VALUE);
    addVariableTransformer.initialise();
    addVariableTransformer.transform(event, ENCODING);

    verifyAdded(event, EXPRESSION_VALUE, PLAIN_STRING_VALUE);
    assertThat(getVariableDataType(event, EXPRESSION_VALUE),
               like(String.class, MediaType.ANY, getDefaultEncoding(mockMuleContext)));
  }

  @Test
  public void testAddVariableWithEncoding() throws InitialisationException, TransformerException {
    addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
    addVariableTransformer.setValue(PLAIN_STRING_VALUE);
    addVariableTransformer.initialise();
    addVariableTransformer.setReturnDataType(DataType.builder().charset(CUSTOM_ENCODING).build());
    addVariableTransformer.transform(event, ENCODING);

    verifyAdded(event, PLAIN_STRING_KEY, PLAIN_STRING_VALUE);
    assertThat(getVariableDataType(event, PLAIN_STRING_KEY), like(String.class, MediaType.ANY, CUSTOM_ENCODING));
  }

  @Test
  public void testAddVariableWithMimeType() throws InitialisationException, TransformerException, MimeTypeParseException {
    addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
    addVariableTransformer.setValue(PLAIN_STRING_VALUE);
    addVariableTransformer.initialise();
    addVariableTransformer.setReturnDataType(DataType.builder().mediaType(APPLICATION_XML).build());
    addVariableTransformer.transform(event, ENCODING);

    verifyAdded(event, PLAIN_STRING_KEY, PLAIN_STRING_VALUE);
    assertThat(getVariableDataType(event, PLAIN_STRING_KEY),
               like(String.class, APPLICATION_XML, getDefaultEncoding(mockMuleContext)));
  }

  protected abstract DataType getVariableDataType(MuleEvent event, String key);

  @Test(expected = IllegalArgumentException.class)
  public void testAddVariableWithNullKey() throws InitialisationException, TransformerException {
    addVariableTransformer.setIdentifier(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddVariableWithEmptyKey() throws InitialisationException, TransformerException {
    addVariableTransformer.setIdentifier("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddVariableWithNullValue() throws InitialisationException, TransformerException {
    addVariableTransformer.setValue(null);
  }

  @Test
  public void testAddVariableWithNullExpressionKeyResult() throws InitialisationException, TransformerException {
    addVariableTransformer.setIdentifier(NULL_EXPRESSION);
    addVariableTransformer.setValue(PLAIN_STRING_VALUE);
    addVariableTransformer.initialise();
    addVariableTransformer.transform(event, ENCODING);
    verifyNotAdded(event);
  }

  @Test
  public void testAddVariableWithNullExpressionValueResult() throws InitialisationException, TransformerException {
    addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
    TypedValue typedValue = new TypedValue(null, DataType.OBJECT);
    when(mockExpressionManager.evaluateTyped(NULL_EXPRESSION, event, null)).thenReturn(typedValue);
    addVariableTransformer.setValue(NULL_EXPRESSION);
    addVariableTransformer.initialise();
    addVariableTransformer.transform(event, ENCODING);
    verifyRemoved(event, PLAIN_STRING_KEY);
  }

  @Test
  public void testAddVariableWithNullPayloadExpressionValueResult() throws InitialisationException, TransformerException {
    addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
    addVariableTransformer.setValue(EXPRESSION);
    TypedValue typedValue = new TypedValue(null, DataType.OBJECT);
    when(mockExpressionManager.evaluateTyped(EXPRESSION, event, null)).thenReturn(typedValue);
    addVariableTransformer.initialise();

    addVariableTransformer.transform(event, ENCODING);

    verifyRemoved(event, PLAIN_STRING_KEY);
  }

  protected abstract void verifyAdded(MuleEvent event, String key, String value);

  protected abstract void verifyNotAdded(MuleEvent mockEvent);

  protected abstract void verifyRemoved(MuleEvent mockEvent, String key);

}
