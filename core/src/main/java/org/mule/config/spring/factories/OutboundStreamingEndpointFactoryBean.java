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

import org.mule.impl.endpoint.OutboundEndpoint;
import org.mule.impl.endpoint.OutboundStreamingEndpoint;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;

/**
 * Spring FactoryBean used to create concrete instances of streaming outbound endpoints
 */
public class OutboundStreamingEndpointFactoryBean extends OutboundEndpointFactoryBean
{

    public OutboundStreamingEndpointFactoryBean()
    {
        super();
    }

    protected UMOImmutableEndpoint doBuildOutboundEndpoint() throws InitialisationException, EndpointException 
    {
        OutboundEndpoint ep = new OutboundStreamingEndpoint();
        configureEndpoint(ep);
        ep.setTransformers(getOutboundTransformers(ep.getConnector(), ep.getEndpointURI()));
        return ep;
    }

}
