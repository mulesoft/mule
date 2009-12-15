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
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;

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
        transformers = global.transformers;
        responseTransformers = global.responseTransformers;
        name = global.name; // this seems a bit odd, but is tested for in the big
                            // spring config test case
        properties = global.properties;
        transactionConfig = global.transactionConfig;
        filter = global.filter;
        deleteUnacceptedMessages = global.deleteUnacceptedMessages;
        securityFilter = global.securityFilter;
        synchronous = global.synchronous;
        responseTimeout = global.responseTimeout;
        encoding = global.encoding;
        retryPolicyTemplate = global.retryPolicyTemplate;
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
        setTransformers(source.getTransformers().isEmpty() ? null : source.getTransformers());
        setResponseTransformers(source.getResponseTransformers().isEmpty() ? null : source.getResponseTransformers());
        setProperties(source.getProperties());
        setTransactionConfig(source.getTransactionConfig());
        setDeleteUnacceptedMessages(source.isDeleteUnacceptedMessages());
        setInitialState(source.getInitialState());
        setResponseTimeout(source.getResponseTimeout());
        setFilter(source.getFilter());
        setSecurityFilter(source.getSecurityFilter());
        setRetryPolicyTemplate(source.getRetryPolicyTemplate());
        setSynchronous(source.isSynchronous());
        setMuleContext(source.getMuleContext());
    }

}
