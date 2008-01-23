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
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;

public class EndpointURIEndpointBuilder extends AbstractEndpointBuilder implements MuleContextAware
{

    public EndpointURIEndpointBuilder()
    {
        // blank
    }

    public EndpointURIEndpointBuilder(EndpointURIEndpointBuilder global) throws EndpointException
    {
        // can't (concisely) use setters where null is a possibility
        // for consistency, set directly on all fields (this also avoids logic in getters)
        uriBuilder = global.uriBuilder;
        connector = global.connector;
        transformers = global.transformers;
        responseTransformers = global.responseTransformers;
        name = global.name; // this seems a bit odd, but is tested for in the big spring config test case
        properties = global.properties;
        transactionConfig = global.transactionConfig;
        filter = global.filter;
        deleteUnacceptedMessages = global.deleteUnacceptedMessages;
        securityFilter = global.securityFilter;
        synchronous = global.synchronous;
        remoteSync = global.remoteSync;
        remoteSyncTimeout = global.remoteSyncTimeout;
        encoding = global.encoding;
        connectionStrategy = global.connectionStrategy;
    }

    public EndpointURIEndpointBuilder(URIBuilder URIBuilder, MuleContext muleContext)
    {
        this.muleContext = muleContext;
        this.uriBuilder = URIBuilder;
    }

    /**
     * @deprecated
     */
    public EndpointURIEndpointBuilder(String address, MuleContext muleContext)
    {
        this(new URIBuilder(address), muleContext);
    }

    /**
     * @deprecated
     */
    public EndpointURIEndpointBuilder(EndpointURI endpointURI, MuleContext muleContext)
    {
        this(new URIBuilder(endpointURI), muleContext);
    }

    /**
     * @deprecated
     */
    public EndpointURIEndpointBuilder(ImmutableEndpoint source, MuleContext muleContext)
    {
        this(source.getEndpointURI(), muleContext);
        setName(source.getName());
        setEncoding(source.getEncoding());
        setConnector(source.getConnector());
        setTransformers(source.getTransformers());
        setResponseTransformers(source.getResponseTransformers());
        setProperties(source.getProperties());
        setTransactionConfig(source.getTransactionConfig());
        setDeleteUnacceptedMessages(source.isDeleteUnacceptedMessages());
        setInitialState(source.getInitialState());
        setRemoteSyncTimeout(source.getRemoteSyncTimeout());
        setRemoteSync(source.isRemoteSync());
        setFilter(source.getFilter());
        setSecurityFilter(source.getSecurityFilter());
        setConnectionStrategy(source.getConnectionStrategy());
        setSynchronous(source.isSynchronous());
        setMuleContext(source.getMuleContext());
    }

}
