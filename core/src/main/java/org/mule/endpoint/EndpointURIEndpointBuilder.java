/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint;

import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;

import java.util.Collections;

public class EndpointURIEndpointBuilder extends AbstractEndpointBuilder
{

    public EndpointURIEndpointBuilder()
    {
        super();
    }

    /**
     * Called from Spring
     * 
     * @param global The global endpoint "Policy"
     */
    public EndpointURIEndpointBuilder(EndpointURIEndpointBuilder global) throws EndpointException
    {
        super();
        
        // can't (concisely) use setters where null is a possibility
        // for consistency, set directly on all fields (this also avoids logic in
        // getters)
        uriBuilder = global.uriBuilder;
        connector = global.connector;
        name = global.name; // this seems a bit odd, but is tested for in the big
                            // spring config test case
        properties = global.properties;
        transactionConfig = global.transactionConfig;
        deleteUnacceptedMessages = global.deleteUnacceptedMessages;
        synchronous = global.synchronous;
        messageExchangePattern = global.messageExchangePattern;
        responseTimeout = global.responseTimeout;
        encoding = global.encoding;
        retryPolicyTemplate = global.retryPolicyTemplate;
        messageProcessors = global.messageProcessors;
        responseMessageProcessors = global.responseMessageProcessors;
        mimeType = global.mimeType;
        disableTransportTransformer = global.disableTransportTransformer;
        transformers = global.transformers;
        responseTransformers = global.responseTransformers;
        redeliveryPolicy = global.redeliveryPolicy;
        setAnnotations(global.getAnnotations());

    }

    public EndpointURIEndpointBuilder(URIBuilder builder)
    {
        super();
        this.uriBuilder = builder;
        this.muleContext = builder.getMuleContext();
    }

    public EndpointURIEndpointBuilder(String address, MuleContext muleContext)
    {
        this(new URIBuilder(address, muleContext));
    }

    protected EndpointURIEndpointBuilder(EndpointURI endpointURI)
    {
        this(new URIBuilder(endpointURI));
    }

    public EndpointURIEndpointBuilder(ImmutableEndpoint source)
    {
        this(source.getEndpointURI());
        setName(source.getName());
        setEncoding(source.getEncoding());
        setConnector(source.getConnector());
        setProperties(source.getProperties());
        setTransactionConfig(source.getTransactionConfig());
        setDeleteUnacceptedMessages(source.isDeleteUnacceptedMessages());
        setInitialState(source.getInitialState());
        setResponseTimeout(source.getResponseTimeout());
        setRetryPolicyTemplate(source.getRetryPolicyTemplate());
        setExchangePattern(source.getExchangePattern());
        setMuleContext(source.getMuleContext());
        setMessageProcessors(source.getMessageProcessors().isEmpty() ? Collections.<MessageProcessor>emptyList() : source.getMessageProcessors());
        setResponseMessageProcessors(source.getResponseMessageProcessors().isEmpty() ? Collections.<MessageProcessor>emptyList() : source.getResponseMessageProcessors());
        setDisableTransportTransformer(source.isDisableTransportTransformer());
        setMimeType(source.getMimeType());
        setRedeliveryPolicy(source.getRedeliveryPolicy());
    }
}
