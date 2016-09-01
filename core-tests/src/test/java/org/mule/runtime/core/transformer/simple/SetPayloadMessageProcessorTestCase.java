/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.transformer.simple;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.expression.ExpressionManager;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;

public class SetPayloadMessageProcessorTestCase extends AbstractMuleContextTestCase {

  private static final String PLAIN_TEXT = "This is a plain text";
  private static final String EXPRESSION = "#[testVariable]";
  private static final Charset CUSTOM_ENCODING = StandardCharsets.UTF_16;

  private SetPayloadMessageProcessor setPayloadMessageProcessor;
  private MuleContext muleContext;
  private MuleMessage muleMessage;
  private MuleEvent muleEvent;
  private ExpressionManager expressionManager;

  @Before
  public void setUp() throws Exception {
    setPayloadMessageProcessor = new SetPayloadMessageProcessor();
    muleContext = mock(MuleContext.class);
    setPayloadMessageProcessor.setMuleContext(muleContext);
    expressionManager = mock(ExpressionManager.class);

    when(muleContext.getExpressionManager()).thenReturn(expressionManager);
    when(muleContext.getConfiguration()).thenReturn(mock(MuleConfiguration.class));
    when(expressionManager.parse(anyString(), any(MuleEvent.class), any(FlowConstruct.class)))
        .thenAnswer(invocation -> (String) invocation.getArguments()[0]);

    muleMessage = MuleMessage.builder().payload("").build();
    Flow flow = getTestFlow();
    muleEvent = MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(muleMessage).flow(flow).build();
  }

  @Test
  public void returnsSameMuleEvent() throws MuleException {
    setPayloadMessageProcessor.setValue(PLAIN_TEXT);
    setPayloadMessageProcessor.initialise();

    MuleEvent response = setPayloadMessageProcessor.process(muleEvent);

    assertThat(response, is(muleEvent));
  }

  @Test
  public void setsNullPayload() throws MuleException {
    setPayloadMessageProcessor.setValue(null);
    setPayloadMessageProcessor.initialise();

    setPayloadMessageProcessor.process(muleEvent);

    assertThat(muleEvent.getMessage().getPayload(), is(nullValue()));
  }

  @Test
  public void setsPlainText() throws MuleException {
    setPayloadMessageProcessor.setValue(PLAIN_TEXT);
    setPayloadMessageProcessor.initialise();

    when(expressionManager.isExpression(PLAIN_TEXT)).thenReturn(false);

    setPayloadMessageProcessor.process(muleEvent);

    assertThat(muleEvent.getMessage().getPayload(), is(PLAIN_TEXT));
  }

  @Test
  public void setsExpressionPayload() throws MuleException {
    setPayloadMessageProcessor.setValue(EXPRESSION);
    when(expressionManager.isExpression(EXPRESSION)).thenReturn(true);
    setPayloadMessageProcessor.initialise();
    TypedValue typedValue = new TypedValue(PLAIN_TEXT, DataType.STRING);
    when(expressionManager.evaluateTyped(EXPRESSION, muleEvent, null)).thenReturn(typedValue);

    setPayloadMessageProcessor.process(muleEvent);

    assertThat(muleEvent.getMessage().getPayload(), is(PLAIN_TEXT));
  }

  @Test
  public void setsDefaultDataTypeForNullPayload() throws MuleException {
    setPayloadMessageProcessor.setValue(null);
    setPayloadMessageProcessor.initialise();

    setPayloadMessageProcessor.process(muleEvent);

    assertThat(muleEvent.getMessage().getDataType(), like(Object.class, MediaType.ANY, null));
  }

  @Test
  public void setsDefaultDataTypeForNonNullValue() throws MuleException {
    setPayloadMessageProcessor.setValue(PLAIN_TEXT);
    setPayloadMessageProcessor.initialise();

    setPayloadMessageProcessor.process(muleEvent);

    assertThat(muleEvent.getMessage().getDataType(), like(String.class, MediaType.ANY, null));
  }

  @Test
  public void setsCustomEncoding() throws MuleException {
    setPayloadMessageProcessor.setValue(PLAIN_TEXT);
    setPayloadMessageProcessor.setDataType(DataType.builder().charset(CUSTOM_ENCODING).build());
    setPayloadMessageProcessor.initialise();

    setPayloadMessageProcessor.process(muleEvent);

    assertThat(muleEvent.getMessage().getDataType(), like(String.class, MediaType.ANY, CUSTOM_ENCODING));
  }

  @Test
  public void setsCustomMimeType() throws MuleException {
    setPayloadMessageProcessor.setValue(PLAIN_TEXT);
    setPayloadMessageProcessor.setDataType(DataType.builder().mediaType(MediaType.APPLICATION_XML).build());
    setPayloadMessageProcessor.initialise();

    setPayloadMessageProcessor.process(muleEvent);

    assertThat(muleEvent.getMessage().getDataType(), like(String.class, MediaType.APPLICATION_XML, null));
  }
}
