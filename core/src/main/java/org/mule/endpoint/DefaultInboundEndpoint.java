/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transport.Connector;
import org.mule.config.MuleManifest;
import org.mule.config.i18n.CoreMessages;

import java.beans.ExceptionListener;
import java.util.List;
import java.util.Map;

public class DefaultInboundEndpoint extends AbstractEndpoint implements InboundEndpoint
{
    private static final long serialVersionUID = -4752772777414636142L;
    private MessageProcessor listener;
    private FlowConstruct flowConstruct;
    private ExceptionListener exceptionListener;

    public DefaultInboundEndpoint(Connector connector,
                                  EndpointURI endpointUri,
                                  String name,
                                  Map properties,
                                  TransactionConfig transactionConfig,
                                  boolean deleteUnacceptedMessage,
                                  MessageExchangePattern messageExchangePattern,
                                  int responseTimeout,
                                  String initialState,
                                  String endpointEncoding,
                                  String endpointBuilderName,
                                  MuleContext muleContext,
                                  RetryPolicyTemplate retryPolicyTemplate,
                                  EndpointMessageProcessorChainFactory messageProcessorsFactory,
                                  List <MessageProcessor> messageProcessors,
                                  List <MessageProcessor> responseMessageProcessors,
                                  boolean disableTransportTransformer,
                                  String mimeType)
    {
        super(connector, endpointUri, name, properties, 
            transactionConfig, deleteUnacceptedMessage,
            messageExchangePattern, responseTimeout, initialState, endpointEncoding, 
            endpointBuilderName, muleContext, retryPolicyTemplate,  messageProcessorsFactory, 
            messageProcessors, responseMessageProcessors, disableTransportTransformer,
            mimeType);
    }

    public MuleMessage request(long timeout) throws Exception
    {
        if (getConnector() != null)
        {
            return getConnector().request(this, timeout);
        }
        else
        {
            // TODO Either remove because this should never happen or i18n the
            // message
            throw new IllegalStateException("The connector on the endpoint: " + toString()
                                            + " is null. Please contact " + MuleManifest.getDevListEmail());
        }
    }

    public void setListener(MessageProcessor listener)
    {
        this.listener = listener;
    }

    public void start() throws MuleException
    {
        try
        {
            if (getMessageProcessorChain() instanceof Startable)
            {
                ((Startable) getMessageProcessorChain()).start();
            }
            getConnector().registerListener(this, getMessageProcessorChain(), flowConstruct);
        }
        catch (Exception e)
        {
            throw new LifecycleException(CoreMessages.failedToStartInboundEndpoint(this), e, this);
        }
    }

    public void stop() throws MuleException
    {
        try
        {
            getConnector().unregisterListener(this, flowConstruct);
            if (getMessageProcessorChain() instanceof Stoppable)
            {
                ((Stoppable) getMessageProcessorChain()).stop();
            }
        }
        catch (Exception e)
        {
            throw new LifecycleException(CoreMessages.failedToStartInboundEndpoint(this), e, this);
        }
    }

    @Override
    public MessageProcessor createMessageProcessorChain() throws MuleException
    {
        EndpointMessageProcessorChainFactory factory = getMessageProcessorsFactory();
        MessageProcessor processorChain = factory.createInboundMessageProcessorChain(this, listener);
        if (processorChain instanceof FlowConstructAware)
        {
            ((FlowConstructAware) processorChain).setFlowConstruct(flowConstruct);
        }
        if (processorChain instanceof Initialisable)
        {
            ((Initialisable) processorChain).initialise();
        }
        return processorChain;
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    public ExceptionListener getExceptionListener()
    {
        return exceptionListener;
    }

    public void setExceptionListener(ExceptionListener exceptionListener)
    {
        this.exceptionListener = exceptionListener;
    }
}
