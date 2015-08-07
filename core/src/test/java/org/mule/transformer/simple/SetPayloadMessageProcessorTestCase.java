/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.simple;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.transformer.DataType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.matcher.DataTypeMatcher;
import org.mule.transformer.types.MimeTypes;
import org.mule.transformer.types.TypedValue;
import org.mule.transport.NullPayload;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SetPayloadMessageProcessorTestCase extends AbstractMuleTestCase
{

    private static final String PLAIN_TEXT = "This is a plain text";
    private static final String EXPRESSION = "#[testVariable]";
    private static final String CUSTOM_ENCODING = "UTF-16";

    private SetPayloadMessageProcessor setPayloadMessageProcessor;
    private MuleContext muleContext;
    private MuleMessage muleMessage;
    private MuleEvent muleEvent;
    private ExpressionManager expressionManager;
    private final ArgumentCaptor<DataType> actualDataType = ArgumentCaptor.forClass(DataType.class);
    private final ArgumentCaptor<Object> actualValue = ArgumentCaptor.forClass(Object.class);

    @Before
    public void setUp()
    {
        setPayloadMessageProcessor = new SetPayloadMessageProcessor();
        muleContext = mock(MuleContext.class);
        setPayloadMessageProcessor.setMuleContext(muleContext);
        expressionManager = mock(ExpressionManager.class);
        muleMessage = mock(MuleMessage.class);

        when(muleContext.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.parse(anyString(), any(MuleMessage.class))).thenAnswer(
                new Answer<String>()
                {
                    @Override
                    public String answer(InvocationOnMock invocation) throws Throwable
                    {

                        return (String) invocation.getArguments()[0];
                    }
                });

        muleEvent = mock(MuleEvent.class);
        when(muleEvent.getMessage()).thenReturn(muleMessage);
    }

    @Test
    public void returnsSameMuleEvent() throws MuleException
    {
        setPayloadMessageProcessor.setValue(PLAIN_TEXT);
        setPayloadMessageProcessor.initialise();

        MuleEvent response = setPayloadMessageProcessor.process(muleEvent);

        assertThat(response, is(muleEvent));
    }

    @Test
    public void setsNullPayload() throws MuleException
    {
        setPayloadMessageProcessor.setValue(null);
        setPayloadMessageProcessor.initialise();

        setPayloadMessageProcessor.process(muleEvent);

        verify(muleMessage).setPayload(actualValue.capture(), Matchers.<DataType<?>>any());

        assertTrue(actualValue.getValue() instanceof NullPayload);
    }

    @Test
    public void setsPlainText() throws MuleException
    {
        setPayloadMessageProcessor.setValue(PLAIN_TEXT);
        setPayloadMessageProcessor.initialise();

        when(expressionManager.isExpression(PLAIN_TEXT)).thenReturn(false);

        setPayloadMessageProcessor.process(muleEvent);

        verify(muleMessage).setPayload(actualValue.capture(), Matchers.<DataType<?>>any());

        assertThat((String) actualValue.getValue(), equalTo(PLAIN_TEXT));
    }

    @Test
    public void setsExpressionPayload() throws MuleException
    {
        setPayloadMessageProcessor.setValue(EXPRESSION);
        when(expressionManager.isExpression(EXPRESSION)).thenReturn(true);
        setPayloadMessageProcessor.initialise();
        TypedValue typedValue = new TypedValue(PLAIN_TEXT, DataType.STRING_DATA_TYPE);
        when(expressionManager.evaluateTyped(EXPRESSION, muleMessage)).thenReturn(typedValue);


        setPayloadMessageProcessor.process(muleEvent);

        verify(muleMessage).setPayload(actualValue.capture(), Matchers.<DataType<?>>any());

        assertThat((String) actualValue.getValue(), equalTo(PLAIN_TEXT));
    }

    @Test
    public void setsDefaultDataTypeForNullPayload() throws MuleException
    {
        setPayloadMessageProcessor.setValue(null);
        setPayloadMessageProcessor.initialise();

        setPayloadMessageProcessor.process(muleEvent);

        verify(muleMessage).setPayload(any(), actualDataType.capture());

        assertThat(actualDataType.getValue(), DataTypeMatcher.like(Object.class, MimeTypes.ANY, null));
    }

    @Test
    public void setsDefaultDataTypeForNonNullValue() throws MuleException
    {
        setPayloadMessageProcessor.setValue(PLAIN_TEXT);
        setPayloadMessageProcessor.initialise();

        setPayloadMessageProcessor.process(muleEvent);

        verify(muleMessage).setPayload(any(), actualDataType.capture());

        assertThat(actualDataType.getValue(), DataTypeMatcher.like(String.class, MimeTypes.ANY, null));
    }

    @Test
    public void setsCustomEncoding() throws MuleException
    {
        setPayloadMessageProcessor.setValue(PLAIN_TEXT);
        setPayloadMessageProcessor.setEncoding(CUSTOM_ENCODING);
        setPayloadMessageProcessor.initialise();

        setPayloadMessageProcessor.process(muleEvent);

        verify(muleMessage).setPayload(any(), actualDataType.capture());

        assertThat(actualDataType.getValue(), DataTypeMatcher.like(String.class, MimeTypes.ANY, CUSTOM_ENCODING));
    }

    @Test
    public void setsCustomMimeType() throws MuleException
    {
        setPayloadMessageProcessor.setValue(PLAIN_TEXT);
        setPayloadMessageProcessor.setMimeType(MimeTypes.APPLICATION_XML);
        setPayloadMessageProcessor.initialise();

        setPayloadMessageProcessor.process(muleEvent);

        verify(muleMessage).setPayload(any(), actualDataType.capture());

        assertThat(actualDataType.getValue(), DataTypeMatcher.like(String.class, MimeTypes.APPLICATION_XML, null));
    }
}