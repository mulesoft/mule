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
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
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
                                   List<Transformer> transformers,
                                   List<Transformer> responseTransformers,
                                   String name,
                                   Map properties,
                                   TransactionConfig transactionConfig,
                                   Filter filter,
                                   boolean deleteUnacceptedMessage,
                                   EndpointSecurityFilter securityFilter,
                                   MessageExchangePattern messageExchangePattern,
                                   int responseTimeout,
                                   String initialState,
                                   String endpointEncoding,
                                   String endpointBuilderName,
                                   MuleContext muleContext,
                                   RetryPolicyTemplate retryPolicyTemplate,
                                   String responsePropertiesList,
                                   EndpointMessageProcessorChainFactory messageProcessorsFactory,
                                   List <MessageProcessor> messageProcessors,
                                   List <MessageProcessor> responseMessageProcessors)
    {
        super(connector, endpointUri, transformers, responseTransformers, name, properties, 
            transactionConfig, filter, deleteUnacceptedMessage, securityFilter, 
            messageExchangePattern, responseTimeout, initialState, endpointEncoding, endpointBuilderName, 
            muleContext, retryPolicyTemplate,  messageProcessorsFactory, messageProcessors, 
            responseMessageProcessors);

        responseProperties = new ArrayList<String>();
        // Propagate the Correlation-related properties from the previous message by default (see EE-1613).
        responseProperties.add(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
        responseProperties.add(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY);
        responseProperties.add(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY);
        responseProperties.add(MuleProperties.MULE_SESSION_PROPERTY);
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

    @Override
    protected MessageProcessor createMessageProcessorChain() throws MuleException
    {
        EndpointMessageProcessorChainFactory factory = getMessageProcessorsFactory();
        return factory.createOutboundMessageProcessorChain(this, 
            ((AbstractConnector) getConnector()).createDispatcherMessageProcessor(this));
    }
}
