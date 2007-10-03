/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.factories;

import org.mule.impl.endpoint.InboundEndpoint;
import org.mule.impl.endpoint.InboundStreamingEndpoint;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;

/**
 * Spring FactoryBean used to create concrete instances of strwaming inbound endpoints
 */
public class InboundStreamingEndpointFactoryBean extends InboundEndpointFactoryBean
{

    public InboundStreamingEndpointFactoryBean()
    {
        super();
    }

    protected UMOImmutableEndpoint doBuildInboundEndpoint() throws InitialisationException, EndpointException 
    {
        InboundEndpoint ep = new InboundStreamingEndpoint();
        configureEndpoint(ep);
        ep.setTransformers(getInboundTransformers(ep.getConnector(), ep.getEndpointURI()));
        ep.setResponseTransformers(getResponseTransformers(ep.getConnector(), ep.getEndpointURI()));
        return ep;
    }
}
