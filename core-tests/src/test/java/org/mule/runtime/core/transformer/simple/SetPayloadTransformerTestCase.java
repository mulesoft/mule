/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class SetPayloadTransformerTestCase extends AbstractMuleTestCase {

  private static final String PLAIN_TEXT = "This is a plain text";
  private static final String EXPRESSION = "#[testVariable]";

  private SetPayloadTransformer setPayloadTransformer;
  private MuleContext mockMuleContext;
  private Event mockMuleEvent;
  private InternalMessage mockMuleMessage;
  private ExpressionLanguage mockExpressionLanguage;

  @Before
  public void setUp() {
    setPayloadTransformer = new SetPayloadTransformer();
    mockMuleContext = mock(MuleContext.class);
    setPayloadTransformer.setMuleContext(mockMuleContext);
    mockExpressionLanguage = mock(ExpressionLanguage.class);
    mockMuleEvent = mock(Event.class);
    mockMuleMessage = mock(InternalMessage.class);

    when(mockMuleEvent.getMessage()).thenReturn(mockMuleMessage);
    when(mockMuleContext.getExpressionLanguage()).thenReturn(mockExpressionLanguage);
    when(mockExpressionLanguage.parse(anyString(), any(Event.class), any(FlowConstruct.class)))
        .thenAnswer(invocation -> (String) invocation.getArguments()[0]);
  }

  @Test
  public void testSetPayloadTransformerNulValue() throws InitialisationException, TransformerException {
    setPayloadTransformer.setValue(null);
    setPayloadTransformer.initialise();

    Object response = setPayloadTransformer.transformMessage(mockMuleEvent, UTF_8);
    assertThat(response, is(nullValue()));
  }

  @Test
  public void testSetPayloadTransformerPlainText() throws InitialisationException, TransformerException {
    setPayloadTransformer.setValue(PLAIN_TEXT);
    setPayloadTransformer.initialise();

    when(mockExpressionLanguage.isExpression(PLAIN_TEXT)).thenReturn(false);

    Object response = setPayloadTransformer.transformMessage(mockMuleEvent, UTF_8);
    assertEquals(PLAIN_TEXT, response);
  }

  @Test
  public void testSetPayloadTransformerExpression() throws InitialisationException, TransformerException {
    setPayloadTransformer.setValue(EXPRESSION);
    when(mockExpressionLanguage.isExpression(EXPRESSION)).thenReturn(true);
    setPayloadTransformer.initialise();
    when(mockExpressionLanguage.evaluate(EXPRESSION, mockMuleEvent, null)).thenReturn(PLAIN_TEXT);

    Object response = setPayloadTransformer.transformMessage(mockMuleEvent, UTF_8);
    assertEquals(PLAIN_TEXT, response);
  }

}
