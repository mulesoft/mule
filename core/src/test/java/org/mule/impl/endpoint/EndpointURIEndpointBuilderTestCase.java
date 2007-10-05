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

import org.mule.MuleServer;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

public class EndpointURIEndpointBuilderTestCase extends AbstractMuleTestCase
{

    public void testBuildInboundEndpoint() throws UMOException
    {
        String uri = "test://address";
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, MuleServer.getManagementContext());
        try
        {
            UMOImmutableEndpoint ep = endpointBuilder.buildInboundEndpoint();
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER, ep.getType());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testBuildOutboundEndpoint() throws UMOException
    {
        String uri = "test://address";
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, MuleServer.getManagementContext());
        try
        {
            UMOImmutableEndpoint ep = endpointBuilder.buildOutboundEndpoint();
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER, ep.getType());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getStackTrace());
        }
    }

    public void testBuildResponseEndpoint() throws UMOException
    {
        String uri = "test://address";
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, MuleServer.getManagementContext());
        try
        {
            UMOImmutableEndpoint ep = endpointBuilder.buildResponseEndpoint();
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_RESPONSE, ep.getType());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getStackTrace());
        }
    }

}
