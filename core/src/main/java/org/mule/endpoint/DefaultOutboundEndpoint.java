/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint;

import org.mule.MessageExchangePattern;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.MessagingExceptionHandlerAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transport.Connector;
import org.mule.processor.AbstractRedeliveryPolicy;
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
    private MessagingExceptionHandler exceptionHandler;

    public DefaultOutboundEndpoint(Connector connector,
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
                                   AbstractRedeliveryPolicy redeliveryPolicy,
                                   String responsePropertiesList,
                                   EndpointMessageProcessorChainFactory messageProcessorsFactory,
                                   List <MessageProcessor> messageProcessors,
                                   List <MessageProcessor> responseMessageProcessors,
                                   boolean disableTransportTransformer,
                                   String endpointMimeType)
    {
        super(connector, endpointUri, name, properties, transactionConfig, 
                deleteUnacceptedMessage, messageExchangePattern, responseTimeout, initialState,
                endpointEncoding, endpointBuilderName, muleContext, retryPolicyTemplate, null,
                messageProcessorsFactory, messageProcessors, responseMessageProcessors, disableTransportTransformer, endpointMimeType);

        if (redeliveryPolicy != null)
        {
            logger.warn("Ignoring redelivery policy set on outbound endpoint " + endpointUri);
        }
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

    @Override
    public boolean isDynamic()
    {
        return false;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleEvent result = getMessageProcessorChain(event.getFlowConstruct()).process(event);
        // A filter in a one-way outbound endpoint (sync or async) should not filter the flow.
        if (!getExchangePattern().hasResponse())
        {
            return VoidMuleEvent.getInstance();
        }
        else
        {
            return result;
        }
    }

    @Override
    protected MessageProcessor createMessageProcessorChain(FlowConstruct flowContruct) throws MuleException
    {
        EndpointMessageProcessorChainFactory factory = getMessageProcessorsFactory();
        MessageProcessor chain = factory.createOutboundMessageProcessorChain(this,
            ((AbstractConnector) getConnector()).createDispatcherMessageProcessor(this));

        if (chain instanceof MuleContextAware)
        {
            ((MuleContextAware) chain).setMuleContext(getMuleContext());
        }
        if (chain instanceof FlowConstructAware)
        {
            ((FlowConstructAware) chain).setFlowConstruct(flowContruct);
        }
        if (chain instanceof Initialisable)
        {
            ((Initialisable) chain).initialise();
        }
        if (chain instanceof MessagingExceptionHandlerAware)
        {
            MessagingExceptionHandler chainExceptionHandler = this.exceptionHandler;
            if (chainExceptionHandler == null)
            {
                chainExceptionHandler = flowContruct != null ? flowContruct.getExceptionListener() : null;
            }
            ((MessagingExceptionHandlerAware) chain).setMessagingExceptionHandler(chainExceptionHandler);
        }

        return chain;
    }

    @Override
    public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler)
    {
       this.exceptionHandler = messagingExceptionHandler;
    }
}
