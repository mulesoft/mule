/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.Connector;
import org.mule.execution.MessageProcessingManager;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.TransportMessageProcessContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;

/**
 * <code>JettyHttpMessageReceiver</code> is a simple http server that can be used to
 * listen for http requests on a particular port
 */
public class JettyHttpMessageReceiver extends AbstractMessageReceiver
{
    private MessageProcessingManager messageProcessingManager;

    public JettyHttpMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
        throws CreateException
    {
        super(connector, flowConstruct, endpoint);
    }
    
    public void processMessage(HttpServletRequest request, HttpServletResponse response)
    {
        final JettyMessageProcessTemplate messageProcessTemplate = new JettyMessageProcessTemplate(request, response, this, getEndpoint().getMuleContext());
        final TransportMessageProcessContext messageProcessContext = new TransportMessageProcessContext(this);
        messageProcessingManager.processMessage(messageProcessTemplate, messageProcessContext);
    }

    public ContinuationsResponseHandler processMessageAsync(HttpServletRequest request, HttpServletResponse response, Continuation continuation)
    {
        final JettyContinuationsMessageProcessTemplate messageProcessTemplate = new JettyContinuationsMessageProcessTemplate(request, response, this, getEndpoint().getMuleContext(), continuation);
        final TransportMessageProcessContext messageProcessContext = new TransportMessageProcessContext(this, getWorkManager());
        messageProcessingManager.processMessage(messageProcessTemplate, messageProcessContext);
        return new ContinuationsResponseHandler(messageProcessTemplate);
    }

    public static class ContinuationsResponseHandler
    {
        private JettyContinuationsMessageProcessTemplate jettyMessageProcessTemplateAndContext;

        public ContinuationsResponseHandler(JettyContinuationsMessageProcessTemplate jettyMessageProcessTemplateAndContext)
        {
            this.jettyMessageProcessTemplateAndContext = jettyMessageProcessTemplateAndContext;
        }

        void complete()
        {
            this.jettyMessageProcessTemplateAndContext.completeProcessingRequest();
        }
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        this.messageProcessingManager = endpoint.getMuleContext().getRegistry().get(MuleProperties.OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER);
    }
}
