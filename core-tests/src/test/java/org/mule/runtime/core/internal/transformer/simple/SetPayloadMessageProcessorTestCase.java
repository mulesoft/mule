/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.transformer.simple;

import static java.nio.charset.StandardCharsets.UTF_16;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.processor.simple.SetPayloadMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

public class SetPayloadMessageProcessorTestCase extends AbstractMuleContextTestCase {

  private static final String PLAIN_TEXT = "This is a plain text";
  private static final String EXPRESSION = "#[testVariable]";
  private static final Charset CUSTOM_ENCODING = UTF_16;

  private SetPayloadMessageProcessor setPayloadMessageProcessor;
  private MuleContext muleContext;
  private ExtendedExpressionManager expressionManager;

  @Before
  public void setUp() throws Exception {
    setPayloadMessageProcessor = new SetPayloadMessageProcessor();
    muleContext = mock(MuleContext.class);
    setPayloadMessageProcessor.setMuleContext(muleContext);
    expressionManager = mock(ExtendedExpressionManager.class);

    when(muleContext.getExpressionManager()).thenReturn(expressionManager);
    when(muleContext.getConfiguration()).thenReturn(mock(MuleConfiguration.class));
    when(expressionManager.parse(anyString(), any(CoreEvent.class), any(ComponentLocation.class)))
        .thenAnswer(invocation -> (String) invocation.getArguments()[0]);
  }

  @Test
  public void setsNullPayload() throws MuleException {
    setPayloadMessageProcessor.setValue(null);
    setPayloadMessageProcessor.initialise();

    CoreEvent response = setPayloadMessageProcessor.process(testEvent());

    assertThat(response.getMessage().getPayload().getValue(), is(nullValue()));
  }

  @Test
  public void setsPlainText() throws MuleException {
    setPayloadMessageProcessor.setValue(PLAIN_TEXT);
    setPayloadMessageProcessor.initialise();

    when(expressionManager.isExpression(PLAIN_TEXT)).thenReturn(false);

    CoreEvent response = setPayloadMessageProcessor.process(testEvent());

    assertThat(response.getMessage().getPayload().getValue(), is(PLAIN_TEXT));
  }

  @Test
  public void setsExpressionPayload() throws MuleException {
    setPayloadMessageProcessor.setValue(EXPRESSION);
    when(expressionManager.isExpression(EXPRESSION)).thenReturn(true);
    setPayloadMessageProcessor.initialise();
    TypedValue typedValue = new TypedValue(PLAIN_TEXT, DataType.STRING);
    when(expressionManager.evaluate(EXPRESSION, testEvent())).thenReturn(typedValue);
    when(expressionManager.evaluate(eq(EXPRESSION), eq(testEvent()), any(CoreEvent.Builder.class), eq(null)))
        .thenReturn(typedValue);

    CoreEvent response = setPayloadMessageProcessor.process(testEvent());

    assertThat(response.getMessage().getPayload().getValue(), is(PLAIN_TEXT));
  }

  @Test
  public void setsDefaultDataTypeForNullPayload() throws MuleException {
    setPayloadMessageProcessor.setValue(null);
    setPayloadMessageProcessor.initialise();

    CoreEvent response = setPayloadMessageProcessor.process(testEvent());

    assertThat(response.getMessage().getPayload().getDataType(), like(Object.class, MediaType.ANY, null));
  }

  @Test
  public void setsDefaultDataTypeForNonNullValue() throws MuleException {
    setPayloadMessageProcessor.setValue(PLAIN_TEXT);
    setPayloadMessageProcessor.initialise();

    setPayloadMessageProcessor.process(testEvent());

    assertThat(testEvent().getMessage().getPayload().getDataType(), like(String.class, MediaType.ANY, null));
  }

  @Test
  public void setsCustomEncoding() throws MuleException {
    setPayloadMessageProcessor.setValue(PLAIN_TEXT);
    setPayloadMessageProcessor.setDataType(DataType.builder().charset(CUSTOM_ENCODING).build());
    setPayloadMessageProcessor.initialise();

    CoreEvent response = setPayloadMessageProcessor.process(testEvent());

    assertThat(response.getMessage().getPayload().getDataType(), like(String.class, MediaType.ANY, CUSTOM_ENCODING));
  }

  @Test
  public void setsCustomMimeType() throws MuleException {
    setPayloadMessageProcessor.setValue(PLAIN_TEXT);
    setPayloadMessageProcessor.setDataType(DataType.builder().mediaType(MediaType.APPLICATION_XML).build());
    setPayloadMessageProcessor.initialise();

    CoreEvent response = setPayloadMessageProcessor.process(testEvent());

    assertThat(response.getMessage().getPayload().getDataType(), like(String.class, MediaType.APPLICATION_XML, null));
  }
}
