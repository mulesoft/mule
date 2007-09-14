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

import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

public class EndpointURIEndpointBuilder extends AbstractEndpointBuilder
{

    public EndpointURIEndpointBuilder(final UMOEndpointURI endpointURI, UMOManagementContext managementContext)
    {
        this(null, endpointURI, null, null, 1, null, null, managementContext);
    }

    public EndpointURIEndpointBuilder(String name,
                                  final UMOEndpointURI endpointURI,
                                  final UMOConnector connector,
                                  final UMOTransformer transformer,
                                  final int createConnector,
                                  final String endpointEncoding,
                                  final Map props,
                                  UMOManagementContext managementContext)
    {
        this.managementContext = managementContext;
        this.name = name;
        this.connector = connector;
        this.createConnector = new Integer(createConnector);
        this.endpointEncoding = endpointEncoding;
        this.endpointURI = endpointURI;
        this.properties = new ConcurrentHashMap();
    }

    protected UMOImmutableEndpoint doBuildInboundEndpoint() throws EndpointException, InitialisationException
    {
        InboundEndpoint ep = new InboundEndpoint();
        configureEndpoint(ep);
        ep.setTransformer(getInboundTransformer(ep.getConnector(),ep.getEndpointURI()));
        ep.setResponseTransformer(getResponseTransformer(ep.getConnector(),ep.getEndpointURI()));
        return ep;
    }

    protected UMOImmutableEndpoint doBuildOutboundEndpoint() throws EndpointException, InitialisationException
    {
        OutboundEndpoint ep = new OutboundEndpoint();
        configureEndpoint(ep);
        ep.setTransformer(getOutboundTransformer(ep.getConnector(),ep.getEndpointURI()));
        ep.setResponseTransformer(getResponseTransformer(ep.getConnector(),ep.getEndpointURI()));
        return ep;
    }

    protected UMOImmutableEndpoint doBuildResponseEndpoint() throws EndpointException, InitialisationException
    {
        ResponseEndpoint ep = new ResponseEndpoint();
        configureEndpoint(ep);
        ep.setTransformer(getInboundTransformer(ep.getConnector(),ep.getEndpointURI()));
        ep.setResponseTransformer(getResponseTransformer(ep.getConnector(),ep.getEndpointURI()));
        return ep;
    }

    public UMOImmutableEndpoint buildEndpoint() throws EndpointException, InitialisationException
    {
        MuleEndpoint ep = new MuleEndpoint();
        configureEndpoint(ep);
        return ep;
    }


}
