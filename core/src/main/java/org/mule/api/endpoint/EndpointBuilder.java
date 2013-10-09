/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    /** @deprecated Use addMessageProcessor() */
    @Deprecated
    void addTransformer(Transformer transformer);

    /** @deprecated Use addResponseMessageProcessor() */
    @Deprecated
    void addResponseTransformer(Transformer transformer);

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
