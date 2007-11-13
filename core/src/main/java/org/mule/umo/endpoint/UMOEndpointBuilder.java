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

import org.mule.config.spring.parsers.specific.URIBuilder;
import org.mule.impl.ManagementContextAware;
import org.mule.providers.ConnectionStrategy;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.security.UMOEndpointSecurityFilter;
import org.mule.umo.transformer.UMOTransformer;

import java.util.List;
import java.util.Map;

/**
 * Constructs endpoints. Transport specific endpoints can easily resolve the UMOEndpoint implementation to be
 * uses, for generic endpoints we can either resolve the transport from uri string or use a default
 * implementation.
 */
public interface UMOEndpointBuilder extends ManagementContextAware, Cloneable
{

    /**
     * Constructs inbound endpoints
     * 
     * @return
     * @throws EndpointException
     * @throws InitialisationException
     */
    UMOImmutableEndpoint buildInboundEndpoint() throws EndpointException, InitialisationException;

    /**
     * Constructs outbound endpoints
     * 
     * @return
     * @throws EndpointException
     * @throws InitialisationException
     */
    UMOImmutableEndpoint buildOutboundEndpoint() throws EndpointException, InitialisationException;

    /**
     * Constructs response endpoints See MULE-2293
     * 
     * @return
     * @throws EndpointException
     * @throws InitialisationException
     */
    UMOImmutableEndpoint buildResponseEndpoint() throws EndpointException, InitialisationException;

    void setConnector(UMOConnector connector);

    void addTransformer(UMOTransformer transformer);

    void setTransformers(List transformers);

    void setResponseTransformers(List responseTransformer);

    void setName(String name);

    void setProperty(Object key, Object value);
    
    void setProperties(Map properties);

    void setTransactionConfig(UMOTransactionConfig transactionConfig);

    void setFilter(UMOFilter filter);

    void setDeleteUnacceptedMessages(boolean deleteUnacceptedMessages);

    void setSecurityFilter(UMOEndpointSecurityFilter securityFilter);

    void setSynchronous(boolean synchronous);

    void setRemoteSync(boolean remoteSync);

    void setRemoteSyncTimeout(int remoteSyncTimeout);

    void setStreaming(boolean streaming);

    void setInitialState(String initialState);

    void setEndpointEncoding(String endpointEncoding);

    void setCreateConnector(int createConnector);

    void setRegistryId(String registryId);

    void setManagementContext(UMOManagementContext managementContext);

    void setConnectionStrategy(ConnectionStrategy connectionStrategy);

    void setURIBuilder(URIBuilder URIBuilder);

    Object clone() throws CloneNotSupportedException;

}
