/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.api.transformer.DataType.STRING_DATA_TYPE;
import static org.mule.module.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.DataType;
import org.mule.config.i18n.CoreMessages;
import org.mule.execution.ResponseCompletionCallback;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.listener.async.HttpResponseReadyCallback;
import org.mule.module.http.internal.listener.async.ResponseStatusCallback;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.NullPayload;

import java.util.Collections;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

@SmallTest
public class HttpMessageProcessorTemplateTestCase extends AbstractMuleTestCase
{

    private static final String TEST_MESSAGE = "";

    @Test
    public void statusCodeOnFailures() throws Exception
    {
        MuleEvent testEvent = createMockEvent();

        HttpResponseReadyCallback responseReadyCallback = mock(HttpResponseReadyCallback.class);
        ArgumentCaptor<HttpResponse> httpResponseCaptor = ArgumentCaptor.forClass(HttpResponse.class);
        doNothing().when(responseReadyCallback).responseReady(httpResponseCaptor.capture(), any(ResponseStatusCallback.class));

        HttpMessageProcessorTemplate httpMessageProcessorTemplate = new HttpMessageProcessorTemplate(
                testEvent,
                mock(MessageProcessor.class),
                responseReadyCallback,
                null,
                HttpResponseBuilder.emptyInstance(mock(MuleContext.class)));

        httpMessageProcessorTemplate.sendFailureResponseToClient(
                new MessagingException(CoreMessages.createStaticMessage(TEST_MESSAGE), testEvent), null);
        assertThat(httpResponseCaptor.getValue().getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void statusCodeOnExceptionBuildingResponse() throws Exception
    {
        MuleEvent testEvent = createMockEvent();

        HttpResponseReadyCallback responseReadyCallback = mock(HttpResponseReadyCallback.class);
        ArgumentCaptor<HttpResponse> httpResponseCaptor = ArgumentCaptor.forClass(HttpResponse.class);
        doNothing().when(responseReadyCallback).responseReady(httpResponseCaptor.capture(), any(ResponseStatusCallback.class));

        HttpMessageProcessorTemplate httpMessageProcessorTemplate = new HttpMessageProcessorTemplate(
                testEvent,
                mock(MessageProcessor.class),
                responseReadyCallback,
                null,
                HttpResponseBuilder.emptyInstance(mock(MuleContext.class)));

        ResponseCompletionCallback responseCompletionCallback = mock(ResponseCompletionCallback.class);
        httpMessageProcessorTemplate.sendResponseToClient(testEvent, responseCompletionCallback);

        verify(responseCompletionCallback).responseSentWithFailure(isA(NullPointerException.class), eq(testEvent));
        assertThat(httpResponseCaptor.getValue().getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void statusCodeOnExceptionSendingResponse() throws Exception
    {
        MuleEvent testEvent = createMockEvent();

        HttpResponseReadyCallback responseReadyCallback = mock(HttpResponseReadyCallback.class);
        RuntimeException expected = new RuntimeException("Some exception");
        doThrow(expected).when(responseReadyCallback).responseReady(any(HttpResponse.class), any(ResponseStatusCallback.class));

        HttpMessageProcessorTemplate httpMessageProcessorTemplate = new HttpMessageProcessorTemplate(
                testEvent,
                mock(MessageProcessor.class),
                responseReadyCallback,
                null,
                HttpResponseBuilder.emptyInstance(mock(MuleContext.class)));

        ResponseCompletionCallback responseCompletionCallback = mock(ResponseCompletionCallback.class);
        httpMessageProcessorTemplate.sendResponseToClient(testEvent, responseCompletionCallback);

        verify(responseCompletionCallback).responseSentWithFailure(expected, testEvent);
    }

    private MuleEvent createMockEvent()
    {
        MuleMessage testMessage = mock(MuleMessage.class);
        when(testMessage.getOutboundPropertyNames()).thenReturn(Collections.<String>emptySet());
        when(testMessage.getPayload()).thenReturn(NullPayload.getInstance());
        DataType datatype = STRING_DATA_TYPE;
        when(testMessage.getDataType()).thenReturn( datatype);

        MuleEvent testEvent = mock(MuleEvent.class);
        when(testEvent.getMessage()).thenReturn(testMessage);

        when(testEvent.getMuleContext()).thenReturn(mock(MuleContext.class, RETURNS_DEEP_STUBS));
        return testEvent;
    }

}
