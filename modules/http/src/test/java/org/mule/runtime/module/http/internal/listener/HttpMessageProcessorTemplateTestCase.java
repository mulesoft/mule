/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;

import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.execution.ResponseCompletionCallback;
import org.mule.runtime.module.http.internal.domain.response.HttpResponse;
import org.mule.runtime.module.http.internal.listener.async.HttpResponseReadyCallback;
import org.mule.runtime.module.http.internal.listener.async.ResponseStatusCallback;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

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
        ArgumentCaptor<HttpResponse> httpResponseCaptor = ArgumentCaptor.forClass(HttpResponse.class);
        doThrow(expected).when(responseReadyCallback).responseReady(httpResponseCaptor.capture(), any(ResponseStatusCallback.class));

        HttpMessageProcessorTemplate httpMessageProcessorTemplate = new HttpMessageProcessorTemplate(
                testEvent,
                mock(MessageProcessor.class),
                responseReadyCallback,
                null,
                HttpResponseBuilder.emptyInstance(mock(MuleContext.class)));

        ResponseCompletionCallback responseCompletionCallback = mock(ResponseCompletionCallback.class);

        try
        {
            httpMessageProcessorTemplate.sendResponseToClient(testEvent, responseCompletionCallback);
            fail("Expected exception");
        }
        catch (RuntimeException e)
        {
            assertThat(e, sameInstance(expected));
        }

        verify(responseCompletionCallback, never()).responseSentWithFailure(expected, testEvent);
        assertThat(httpResponseCaptor.getValue().getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    private MuleEvent createMockEvent() throws MuleException
    {
        MuleMessage testMessage = MuleMessage.builder().payload("").build();

        MuleEvent testEvent = mock(MuleEvent.class);
        when(testEvent.getMessage()).thenReturn(testMessage);
        when(testEvent.getMuleContext()).thenReturn(mock(MuleContext.class, RETURNS_DEEP_STUBS));
        when(testEvent.getMessageAsBytes()).thenReturn("".getBytes(UTF_8));
        return testEvent;
    }

}
