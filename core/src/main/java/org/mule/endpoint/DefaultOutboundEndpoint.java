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
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorsFactory;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transport.Connector;
import org.mule.processor.builder.ChainMessageProcessorBuilder;
import org.mule.transport.AbstractConnector;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DefaultOutboundEndpoint extends AbstractEndpoint implements OutboundEndpoint
{
    private static final long serialVersionUID = 8860985949279708638L;

    private List<String> responseProperties;

    public DefaultOutboundEndpoint(Connector connector,
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
                                   String responsePropertiesList,
                                   List <MessageProcessor> messageProcessors,
                                   List <MessageProcessor> responseMessageProcessors)
    {
        super(connector, endpointUri, transformers, responseTransformers, name, properties, transactionConfig, filter,
                deleteUnacceptedMessage, securityFilter, synchronous, responseTimeout, initialState,
                endpointEncoding, endpointBuilderName, muleContext, retryPolicyTemplate, messageProcessors, responseMessageProcessors);

        responseProperties = new ArrayList<String>();
        // Propagate the Correlation-related properties from the previous message by default (see EE-1613).
        responseProperties.add(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
        responseProperties.add(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY);
        responseProperties.add(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY);
        // Add any additional properties specified by the user.
        String[] props = StringUtils.splitAndTrim(responsePropertiesList, ",");
        if (props != null)
        {
            responseProperties.addAll(Arrays.asList(props));
        }
    }

    public List<String> getResponseProperties()
    {
        return responseProperties;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return getMessageProcessorChain().process(event);
    }

    protected MessageProcessor createMessageProcessorChain() throws MuleException
    {
        MessageProcessorsFactory factory = ((AbstractConnector) getConnector()).getMessageProcessorsFactory();
        
        // -- REQUEST CHAIN --
        ChainMessageProcessorBuilder outboundChainBuilder = new ChainMessageProcessorBuilder();
        outboundChainBuilder.setName("Outbound endpoint request chain");
        // Default MPs
        outboundChainBuilder.chain(factory.createOutboundMessageProcessors(this));
        // Configured MPs (if any)
        outboundChainBuilder.chain(getMessageProcessors());
        
        // -- OUTBOUND ROUTER --
        outboundChainBuilder.chain(((AbstractConnector) getConnector()).createDispatcherMessageProcessor(this));
        
        // -- RESPONSE CHAIN --
        ChainMessageProcessorBuilder responseChainBuilder = new ChainMessageProcessorBuilder();
        responseChainBuilder.setName("Outbound endpoint response chain");
        // Default MPs
        responseChainBuilder.chain(factory.createOutboundResponseMessageProcessors(this));
        // Configured MPs (if any)
        responseChainBuilder.chain(getResponseMessageProcessors());

        // Compose request and response chains. We do this so that if the request
        // chain returns early the response chain is still invoked.
        ChainMessageProcessorBuilder compositeChainBuilder = new ChainMessageProcessorBuilder();
        compositeChainBuilder.setName("Outbound endpoint request/response composite chain");
        compositeChainBuilder.chain(outboundChainBuilder.build(), responseChainBuilder.build());
        return compositeChainBuilder.build();
    }
}
