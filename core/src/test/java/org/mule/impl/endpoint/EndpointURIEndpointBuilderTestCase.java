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
import org.mule.impl.registry.TransientRegistry;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

public class EndpointURIEndpointBuilderTestCase extends AbstractMuleTestCase
{

    public void testBuildInboundEndpoint() throws UMOException
    {
        TransientRegistry r=TransientRegistry.createNew();
        String uri="test://address";
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(new MuleEndpointURI(uri),MuleServer.getManagementContext());
        try
        {
            UMOImmutableEndpoint ep= endpointBuilder.buildInboundEndpoint();
        }
        catch (Exception e)
        {
            fail("Unexpected exception: "+e.getMessage());
        }
    }

    public void testBuildOutboundEndpoint() throws UMOException
    {
        TransientRegistry.createNew();
        String uri="test://address";
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(new MuleEndpointURI(uri),MuleServer.getManagementContext());
        try
        {
            UMOImmutableEndpoint ep= endpointBuilder.buildOutboundEndpoint();
        }
        catch (Exception e)
        {
            fail("Unexpected exception: "+e.getStackTrace());
        }
    }

    public void testBuildResponseEndpoint() throws UMOException
    {
        TransientRegistry.createNew();
        String uri="test://address";
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(new MuleEndpointURI(uri),MuleServer.getManagementContext());
        try
        {
            UMOImmutableEndpoint ep= endpointBuilder.buildResponseEndpoint();
        }
        catch (Exception e)
        {
            fail("Unexpected exception: "+e.getStackTrace());
        }
    }

}


