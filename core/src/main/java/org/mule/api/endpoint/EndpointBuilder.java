/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.endpoint;

import org.mule.MessageExchangePattern;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.endpoint.URIBuilder;

import java.util.List;
import java.util.Map;

/**
 * Constructs endpoints. Transport specific endpoints can easily resolve the Endpoint implementation to be
 * uses, for generic endpoints we can either resolve the transport from uri string or use a default
 * implementation.
 */
public interface EndpointBuilder extends MuleContextAware, Cloneable
{
    /**
     * Constructs inbound endpoints
     *
     * @throws EndpointException
     * @throws InitialisationException
     */
    InboundEndpoint buildInboundEndpoint() throws EndpointException, InitialisationException;

    /**
     * Constructs outbound endpoints
     *
     * @throws EndpointException
     * @throws InitialisationException
     */
    OutboundEndpoint buildOutboundEndpoint() throws EndpointException, InitialisationException;

    void setConnector(Connector connector);

    /** @deprecated Use setMessageProcessors() */
    @Deprecated
    void setTransformers(List<Transformer> transformers);

    /** @deprecated Use setResponseMessageProcessors() */
    @Deprecated
    void setResponseTransformers(List<Transformer> responseTransformer);

    void setName(String name);

    void setProperty(Object key, Object value);

    void setProperties(Map<Object, Object> properties);

    void setTransactionConfig(TransactionConfig transactionConfig);

    void setDeleteUnacceptedMessages(boolean deleteUnacceptedMessages);

    void setExchangePattern(MessageExchangePattern mep);

    void setResponseTimeout(int responseTimeout);

    void setInitialState(String initialState);

    void setEncoding(String encoding);

    void setRegistryId(String registryId);

    void setRetryPolicyTemplate(RetryPolicyTemplate retryPolicyTemplate);

    void setMessageProcessors(List <MessageProcessor> messageProcessors);

    void addMessageProcessor(MessageProcessor messageProcessor);

    void setResponseMessageProcessors(List <MessageProcessor> responseMessageProcessors);

    void addResponseMessageProcessor(MessageProcessor responseMessageProcessor);

    void setDisableTransportTransformer(boolean disableTransportTransformer);

    void setURIBuilder(URIBuilder URIBuilder);

    Object clone() throws CloneNotSupportedException;
}
