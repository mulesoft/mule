/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.execution.AsyncResponseFlowProcessingPhaseTemplate;
import org.mule.execution.ResponseCompletionCallback;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.listener.async.HttpResponseReadyCallback;
import org.mule.module.http.internal.listener.async.ResponseStatusCallback;

public class HttpMessageProcessorTemplate implements AsyncResponseFlowProcessingPhaseTemplate
{

    private MuleEvent sourceMuleEvent;
    private MessageProcessor messageProcessor;
    private HttpResponseReadyCallback responseReadyCallback;
    private HttpResponseBuilder responseBuilder;
    private HttpResponseBuilder errorResponseBuilder;

    public HttpMessageProcessorTemplate(MuleEvent sourceMuleEvent,
                                        MessageProcessor messageProcessor,
                                        HttpResponseReadyCallback responseReadyCallback,
                                        HttpResponseBuilder responseBuilder,
                                        HttpResponseBuilder errorResponseBuilder)
    {
        this.sourceMuleEvent = sourceMuleEvent;
        this.messageProcessor = messageProcessor;
        this.responseBuilder = responseBuilder;
        this.errorResponseBuilder = errorResponseBuilder;
        this.responseReadyCallback = responseReadyCallback;
    }

    @Override
    public MuleEvent getMuleEvent() throws MuleException
    {
        return this.sourceMuleEvent;
    }

    @Override
    public MuleEvent routeEvent(MuleEvent muleEvent) throws MuleException
    {
        return messageProcessor.process(muleEvent);
    }

    @Override
    public void afterFailureProcessingFlow(Exception exception)
    {

    }

    @Override
    public void sendResponseToClient(MuleEvent muleEvent, ResponseCompletionCallback responseCompletationCallback) throws MuleException
    {
        try
        {
            final HttpResponse httpResponse = responseBuilder.build(new org.mule.module.http.internal.domain.response.HttpResponseBuilder(), muleEvent);
            responseReadyCallback.responseReady(httpResponse, getResponseFailureCallback(responseCompletationCallback, muleEvent));
        }
        catch (Exception e)
        {
            responseCompletationCallback.responseSentWithFailure(e, muleEvent);
        }
    }

    private ResponseStatusCallback getResponseFailureCallback(final ResponseCompletionCallback responseCompletationCallback, final MuleEvent muleEvent)
    {
        return new ResponseStatusCallback()
        {
            @Override
            public void responseSendFailure(Throwable throwable)
            {
                responseCompletationCallback.responseSentWithFailure(getException(throwable), muleEvent);
            }

            @Override
            public void responseSendSuccessfully()
            {
                responseCompletationCallback.responseSentSuccessfully();
            }
        };
    }

    private Exception getException(Throwable throwable)
    {
        if (throwable instanceof Exception)
        {
            return (Exception) throwable;
        }
        return new Exception(throwable);
    }

    @Override
    public void sendFailureResponseToClient(MessagingException messagingException, ResponseCompletionCallback responseCompletationCallback) throws MuleException
    {
        final org.mule.module.http.internal.domain.response.HttpResponseBuilder failureResponseBuilder = new org.mule.module.http.internal.domain.response.HttpResponseBuilder().setStatusCode(500).setReasonPhrase("Internal Server Error");
        final HttpResponse response = errorResponseBuilder.build(failureResponseBuilder, messagingException.getEvent());
        responseReadyCallback.responseReady(response, getResponseFailureCallback(responseCompletationCallback, messagingException.getEvent()));
    }

}
