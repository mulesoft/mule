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

import java.util.List;
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
                                  final List transformers,
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
        ep.setTransformers(getInboundTransformers(ep.getConnector(), ep.getEndpointURI()));
        ep.setResponseTransformers(getResponseTransformers(ep.getConnector(), ep.getEndpointURI()));
        return ep;
    }

    protected UMOImmutableEndpoint doBuildOutboundEndpoint() throws EndpointException, InitialisationException
    {
        OutboundEndpoint ep = new OutboundEndpoint();
        configureEndpoint(ep);
        ep.setTransformers(getOutboundTransformers(ep.getConnector(), ep.getEndpointURI()));
        ep.setResponseTransformers(getResponseTransformers(ep.getConnector(), ep.getEndpointURI()));
        return ep;
    }

    protected UMOImmutableEndpoint doBuildResponseEndpoint() throws EndpointException, InitialisationException
    {
        ResponseEndpoint ep = new ResponseEndpoint();
        configureEndpoint(ep);
        ep.setTransformers(getInboundTransformers(ep.getConnector(), ep.getEndpointURI()));
        ep.setResponseTransformers(getResponseTransformers(ep.getConnector(), ep.getEndpointURI()));
        return ep;
    }

    public UMOImmutableEndpoint buildEndpoint() throws EndpointException, InitialisationException
    {
        MuleEndpoint ep = new MuleEndpoint();
        configureEndpoint(ep);
        return ep;
    }


}
