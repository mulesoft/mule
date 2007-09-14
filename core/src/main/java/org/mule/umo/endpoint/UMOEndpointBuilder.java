/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.endpoint;

import org.mule.providers.ConnectionStrategy;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.security.UMOEndpointSecurityFilter;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Map;

/**
 * Constructs endpoints. Transport specific endpoints can easily resolve the UMOEndpoint implementation to be
 * uses, for generic endpoints we can either resolve the transport from uri string or use a default
 * implementation.
 */
public interface UMOEndpointBuilder
{

    /**
     * Constructs inbound endpoints
     * 
     * @param uri
     * @return
     * @throws EndpointException
     * @throws InitialisationException
     */
    public abstract UMOImmutableEndpoint buildInboundEndpoint()
        throws EndpointException, InitialisationException;

    /**
     * Constructs outbound endpoints
     * 
     * @param uri
     * @return
     * @throws EndpointException
     * @throws InitialisationException
     */
    public abstract UMOImmutableEndpoint buildOutboundEndpoint()
        throws EndpointException, InitialisationException;

    /**
     * Constructs response endpoints See MULE-2293
     * 
     * @param uri
     * @return
     * @throws EndpointException
     * @throws InitialisationException
     */
    public abstract UMOImmutableEndpoint buildResponseEndpoint()
        throws EndpointException, InitialisationException;
    
    public abstract UMOEndpointBuilder setConnector(UMOConnector connector);

    public abstract UMOEndpointBuilder setTransformer(UMOTransformer transformer);

    public abstract UMOEndpointBuilder setResponseTransformer(UMOTransformer responseTransformer);

    public abstract UMOEndpointBuilder setName(String name);

    public abstract UMOEndpointBuilder setProperties(Map properties);

    public abstract UMOEndpointBuilder setTransactionConfig(UMOTransactionConfig transactionConfig);

    public abstract UMOEndpointBuilder setFilter(UMOFilter filter);

    public abstract UMOEndpointBuilder setDeleteUnacceptedMessages(boolean deleteUnacceptedMessages);

    public abstract UMOEndpointBuilder setSecurityFilter(UMOEndpointSecurityFilter securityFilter);

    public abstract UMOEndpointBuilder setSynchronous(Boolean synchronous);

    public abstract UMOEndpointBuilder setRemoteSync(Boolean remoteSync);

    public abstract UMOEndpointBuilder setRemoteSyncTimeout(int remoteSyncTimeout);

    public abstract UMOEndpointBuilder setStreaming(boolean streaming);

    public abstract UMOEndpointBuilder setInitialState(String initialState);

    public abstract UMOEndpointBuilder setEndpointEncoding(String endpointEncoding);

    public abstract UMOEndpointBuilder setCreateConnector(int createConnector);

    public abstract UMOEndpointBuilder setRegistryId(String registryId);

    public abstract UMOEndpointBuilder setManagementContext(UMOManagementContext managementContext);

    public abstract UMOEndpointBuilder setConnectionStrategy(ConnectionStrategy connectionStrategy);

}
