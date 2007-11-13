/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.endpoint;

import org.mule.config.spring.parsers.specific.URIBuilder;
import org.mule.impl.ManagementContextAware;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;

public class EndpointURIEndpointBuilder extends AbstractEndpointBuilder implements ManagementContextAware
{

    public EndpointURIEndpointBuilder()
    {
        super();
    }

    public EndpointURIEndpointBuilder(URIBuilder URIBuilder, UMOManagementContext managementContext)
    {
        this.managementContext = managementContext;
        this.uriBuilder = URIBuilder;
    }

    /**
     * @deprecated
     */
    public EndpointURIEndpointBuilder(String address, UMOManagementContext managementContext)
    {
        this(new URIBuilder(address), managementContext);
    }

    /**
     * @deprecated
     */
    public EndpointURIEndpointBuilder(UMOEndpointURI endpointURI, UMOManagementContext managementContext)
    {
        this(new URIBuilder(endpointURI), managementContext);
    }

    /**
     * @deprecated
     */
    public EndpointURIEndpointBuilder(UMOImmutableEndpoint source, UMOManagementContext managementContext)
    {
        this(source.getEndpointURI(), managementContext);
        setName(source.getName());
        setEndpointEncoding(source.getEncoding());
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
        setCreateConnector(source.getCreateConnector());
        setManagementContext(source.getManagementContext());
    }

}
