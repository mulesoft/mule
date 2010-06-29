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

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.Pattern;
import org.mule.api.config.ConfigurationException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorsFactory;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transport.Connector;
import org.mule.config.MuleManifest;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.processor.builder.ChainMessageProcessorBuilder;
import org.mule.transport.AbstractConnector;

import java.util.List;
import java.util.Map;

public class DefaultInboundEndpoint extends AbstractEndpoint implements InboundEndpoint
{
    private static final long serialVersionUID = -4752772777414636142L;
    private MessageProcessor listener;
    private Pattern pattern;

    public DefaultInboundEndpoint(Connector connector,
                                  EndpointURI endpointUri,
                                  List transformers,
                                  List responseTransformers,
                                  String name,
                                  Map properties,
                                  TransactionConfig transactionConfig,
                                  Filter filter,
                                  boolean deleteUnacceptedMessage,
                                  EndpointSecurityFilter securityFilter,
                                  boolean synchronous,
                                  int responseTimeout,
                                  String initialState,
                                  String endpointEncoding,
                                  String endpointBuilderName,
                                  MuleContext muleContext,
                                  RetryPolicyTemplate retryPolicyTemplate,
                                  List <MessageProcessor> messageProcessors,
                                  List <MessageProcessor> responseMessageProcessors)
    {
        super(connector, endpointUri, transformers, responseTransformers, name, properties, transactionConfig, filter,
            deleteUnacceptedMessage, securityFilter, synchronous, responseTimeout, initialState,
            endpointEncoding, endpointBuilderName, muleContext, retryPolicyTemplate, messageProcessors, responseMessageProcessors);
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
            getConnector().registerListener(this, getMessageProcessorChain(), pattern);
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
            getConnector().unregisterListener(this);
        }
        catch (Exception e)
        {
            throw new LifecycleException(CoreMessages.failedToStartInboundEndpoint(this), e, this);
        }
    }

    public MessageProcessor createMessageProcessorChain() throws MuleException
    {
        MessageProcessorsFactory factory = ((AbstractConnector) getConnector()).getMessageProcessorsFactory();
        
        // -- REQUEST CHAIN --
        ChainMessageProcessorBuilder requestChainBuilder = new ChainMessageProcessorBuilder();
        requestChainBuilder.setName("Inbound endpoint request pipeline");
        // Default MPs
        requestChainBuilder.chain(factory.createInboundMessageProcessors(this));
        // Configured MPs (if any)
        requestChainBuilder.chain(getMessageProcessors());
        
        // -- INVOKE SERVICE --
        if (listener == null)
        {
            throw new ConfigurationException(MessageFactory.createStaticMessage("No listener (target) has been set for this endpoint"));
        }
        requestChainBuilder.chain(listener);

        // -- RESPONSE CHAIN --
        ChainMessageProcessorBuilder responseChainBuilder = new ChainMessageProcessorBuilder();
        responseChainBuilder.setName("Inbound endpoint response pipeline");
        // Default MPs
        responseChainBuilder.chain(factory.createInboundResponseMessageProcessors(this));
        // Configured MPs (if any)
        responseChainBuilder.chain(getResponseMessageProcessors());

        // Compose request and response chains. We do this so that if the request
        // chain returns early the response chain is still invoked.
        ChainMessageProcessorBuilder inboundChainBuilder = new ChainMessageProcessorBuilder();
        inboundChainBuilder.setName("Inbound endpoint request/response composite pipeline");
        inboundChainBuilder.chain(requestChainBuilder.build(), responseChainBuilder.build());

        return inboundChainBuilder.build();
    }

    public void setPattern(Pattern pattern)
    {
        this.pattern = pattern;
    }
}
