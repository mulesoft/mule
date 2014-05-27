/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.WorkManager;
import org.mule.execution.EndPhaseTemplate;
import org.mule.transport.AbstractMessageReceiver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;

public class JettyContinuationsMessageProcessTemplate extends JettyMessageProcessTemplate implements EndPhaseTemplate
{

    private final Continuation continuation;
    private MuleEvent responseForClient;
    private MessagingException failureResponseForClient;
    private boolean emptyResponseReceived;
    private boolean discardMessage;

    public JettyContinuationsMessageProcessTemplate(HttpServletRequest request, HttpServletResponse response, AbstractMessageReceiver messageReceiver, MuleContext muleContext, Continuation continuation)
    {
        super(request, response, messageReceiver, muleContext);
        this.continuation = continuation;
    }

    @Override
    public void sendResponseToClient(MuleEvent muleEvent) throws MuleException
    {
        if (muleEvent == null)
        {
            emptyResponseReceived = true;
        }
        else
        {
            responseForClient = muleEvent;
        }
    }

    @Override
    public void sendFailureResponseToClient(MessagingException messagingException) throws MuleException
    {
        this.failureResponseForClient = messagingException;
    }

    public void completeProcessingRequest()
    {
        try
        {
            if (discardMessage)
            {
                super.discardMessageOnThrottlingExceeded();
            }
            else if (emptyResponseReceived)
            {
               getServletResponseWriter().writeEmptyResponse(getServletResponse(), null);
            }
            else if (responseForClient != null)
            {
                super.sendResponseToClient(responseForClient);
            }
            else
            {
                super.sendFailureResponseToClient(failureResponseForClient);
            }
        }
        catch (Exception e)
        {
            getMuleContext().getExceptionListener().handleException(e);
        }
    }

    @Override
    public void discardMessageOnThrottlingExceeded() throws MuleException
    {
        this.discardMessage = true;
    }

    @Override
    public void messageProcessingEnded()
    {
        this.continuation.resume();
    }
}
