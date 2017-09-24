/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.DataType.STRING;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class SetPayloadTransformerTestCase extends AbstractMuleTestCase {

  private static final String PLAIN_TEXT = "This is a plain text";
  private static final String EXPRESSION = "#[mel:testVariable]";

  private SetPayloadTransformer setPayloadTransformer;
  private MuleContext mockMuleContext;
  private CoreEvent mockMuleEvent;
  private InternalMessage mockMuleMessage;
  private ExtendedExpressionManager mockExpressionManager;

  @Before
  public void setUp() {
    setPayloadTransformer = new SetPayloadTransformer();
    mockMuleContext = mock(MuleContext.class);
    setPayloadTransformer.setMuleContext(mockMuleContext);
    mockExpressionManager = mock(ExtendedExpressionManager.class);
    mockMuleEvent = mock(CoreEvent.class);
    mockMuleMessage = mock(InternalMessage.class);

    when(mockMuleEvent.getMessage()).thenReturn(mockMuleMessage);
    when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
    when(mockExpressionManager.parse(anyString(), any(CoreEvent.class), any(ComponentLocation.class)))
        .thenAnswer(invocation -> invocation.getArguments()[0]);
  }

  @Test
  public void testSetPayloadTransformerNulValue() throws InitialisationException, MessageTransformerException {
    setPayloadTransformer.setValue(null);
    setPayloadTransformer.initialise();

    Object response = setPayloadTransformer.transformMessage(mockMuleEvent, UTF_8);
    assertThat(response, is(nullValue()));
  }

  @Test
  public void testSetPayloadTransformerPlainText() throws InitialisationException, MessageTransformerException {
    setPayloadTransformer.setValue(PLAIN_TEXT);
    setPayloadTransformer.initialise();

    when(mockExpressionManager.isExpression(PLAIN_TEXT)).thenReturn(false);

    Object response = setPayloadTransformer.transformMessage(mockMuleEvent, UTF_8);
    assertThat(response, is(PLAIN_TEXT));
  }

  @Test
  public void testSetPayloadTransformerExpression() throws InitialisationException, MessageTransformerException {
    setPayloadTransformer.setValue(EXPRESSION);
    when(mockExpressionManager.isExpression(EXPRESSION)).thenReturn(true);
    setPayloadTransformer.initialise();
    TypedValue typedValue = new TypedValue<>(PLAIN_TEXT, STRING);
    when(mockExpressionManager.evaluate(EXPRESSION, mockMuleEvent)).thenReturn(typedValue);

    Object response = setPayloadTransformer.transformMessage(mockMuleEvent, UTF_8);
    assertThat(response, is(PLAIN_TEXT));
  }
}
