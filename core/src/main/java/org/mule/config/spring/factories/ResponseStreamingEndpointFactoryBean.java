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

import org.mule.impl.endpoint.ResponseEndpoint;
import org.mule.impl.endpoint.ResponseStreamingEndpoint;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * Spring FactoryBean used to create concrete instances of streaming response endpoints
 */
public class ResponseStreamingEndpointFactoryBean extends OutboundEndpointFactoryBean
{

    public ResponseStreamingEndpointFactoryBean()
    {
        super();
    }

    protected UMOImmutableEndpoint doBuildResonseEndpoint() throws UMOException
    {
        ResponseEndpoint ep = new ResponseStreamingEndpoint();
        configureEndpoint(ep);
        ep.setTransformers(getInboundTransformers(ep.getConnector(), ep.getEndpointURI()));
        return ep;
    }

}
