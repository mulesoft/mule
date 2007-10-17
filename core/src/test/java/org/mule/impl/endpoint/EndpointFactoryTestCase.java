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

import org.mule.impl.registry.TransientRegistry;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOEndpointFactory;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

public class EndpointFactoryTestCase extends AbstractMuleTestCase
{

    public void testCreateInboundEndpoint() throws UMOException
    {
        String uri = "test://address";
        UMOEndpointFactory endpointFactory = new EndpointFactory();
        try
        {
            UMOImmutableEndpoint ep = endpointFactory.getInboundEndpoint(uri, managementContext);
            assertEquals(InboundEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER, ep.getType());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateInboundEndpointFromGlobalEndpoint() throws UMOException
    {
        managementContext.getRegistry().registerEndpointBuilder("myGlobalEndpoint",
            new EndpointURIEndpointBuilder("test://address", managementContext), managementContext);
        String uri = "myGlobalEndpoint";
        UMOEndpointFactory endpointFactory = new EndpointFactory();
        try
        {
            UMOImmutableEndpoint ep = endpointFactory.getInboundEndpoint(uri, managementContext);
            assertEquals(InboundEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER, ep.getType());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateInboundEndpointFromNamedConcreteEndpoint() throws UMOException
    {
        managementContext.getRegistry().registerEndpointBuilder("&myNamedConcreateEndpoint",
            new EndpointURIEndpointBuilder("test://address", managementContext), managementContext);
        String uri = "&myNamedConcreateEndpoint";
        UMOEndpointFactory endpointFactory = new EndpointFactory();
        try
        {
            UMOImmutableEndpoint ep = endpointFactory.getInboundEndpoint(uri, managementContext);
            assertEquals(InboundEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER, ep.getType());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateOutboundEndpoint() throws UMOException
    {
        String uri = "test://address";
        UMOEndpointFactory endpointFactory = new EndpointFactory();
        try
        {
            UMOImmutableEndpoint ep = endpointFactory.getOutboundEndpoint(uri, managementContext);
            assertEquals(OutboundEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER, ep.getType());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateoutboundEndpointFromGlobalEndpoint() throws UMOException
    {
        managementContext.getRegistry().registerEndpointBuilder("myGlobalEndpoint",
            new EndpointURIEndpointBuilder("test://address", managementContext), managementContext);
        String uri = "myGlobalEndpoint";
        UMOEndpointFactory endpointFactory = new EndpointFactory();
        try
        {
            UMOImmutableEndpoint ep = endpointFactory.getOutboundEndpoint(uri, managementContext);
            assertEquals(OutboundEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER, ep.getType());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateoutboundEndpointFromNamedConcreteEndpoint() throws UMOException
    {
        managementContext.getRegistry().registerEndpointBuilder("&myNamedConcreateEndpoint",
            new EndpointURIEndpointBuilder("test://address", managementContext), managementContext);
        String uri = "&myNamedConcreateEndpoint";
        UMOEndpointFactory endpointFactory = new EndpointFactory();
        try
        {
            UMOImmutableEndpoint ep = endpointFactory.getOutboundEndpoint(uri, managementContext);
            assertEquals(OutboundEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER, ep.getType());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateResponseEndpoint() throws UMOException
    {
        String uri = "test://address";
        UMOEndpointFactory endpointFactory = new EndpointFactory();
        try
        {
            UMOImmutableEndpoint ep = endpointFactory.getResponseEndpoint(uri, managementContext);
            assertEquals(ResponseEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_RESPONSE, ep.getType());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateResponseEndpointFromGlobalEndpoint() throws UMOException
    {
        managementContext.getRegistry().registerEndpointBuilder("myGlobalEndpoint",
            new EndpointURIEndpointBuilder("test://address", managementContext), managementContext);
        String uri = "myGlobalEndpoint";
        UMOEndpointFactory endpointFactory = new EndpointFactory();
        try
        {
            UMOImmutableEndpoint ep = endpointFactory.getResponseEndpoint(uri, managementContext);
            assertEquals(ResponseEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_RESPONSE, ep.getType());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateResponseEndpointFromNamedConcreteEndpoint() throws UMOException
    {
        managementContext.getRegistry().registerEndpointBuilder("&myNamedConcreateEndpoint",
            new EndpointURIEndpointBuilder("test://address", managementContext), managementContext);
        String uri = "&myNamedConcreateEndpoint";
        UMOEndpointFactory endpointFactory = new EndpointFactory();
        try
        {
            UMOImmutableEndpoint ep = endpointFactory.getResponseEndpoint(uri, managementContext);
            assertEquals(ResponseEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_RESPONSE, ep.getType());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateInboundEndpointWithBuilder() throws UMOException
    {
        UMOEndpointBuilder builder = new EndpointURIEndpointBuilder("test://address", managementContext);
        UMOEndpointFactory endpointFactory = new EndpointFactory();
        try
        {
            UMOImmutableEndpoint ep = endpointFactory.getInboundEndpoint(builder, managementContext);
            assertEquals(InboundEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER, ep.getType());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateOutboundEndpointWithBuilder() throws UMOException
    {
        UMOEndpointBuilder builder = new EndpointURIEndpointBuilder("test://address", managementContext);
        UMOEndpointFactory endpointFactory = new EndpointFactory();
        try
        {
            UMOImmutableEndpoint ep = endpointFactory.getOutboundEndpoint(builder, managementContext);
            assertEquals(OutboundEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER, ep.getType());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateResponseEndpointWithBuilder() throws UMOException
    {
        UMOEndpointBuilder builder = new EndpointURIEndpointBuilder("test://address", managementContext);
        UMOEndpointFactory endpointFactory = new EndpointFactory();
        try
        {
            UMOImmutableEndpoint ep = endpointFactory.getResponseEndpoint(builder, managementContext);
            assertEquals(ResponseEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertEquals(UMOImmutableEndpoint.ENDPOINT_TYPE_RESPONSE, ep.getType());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateEndpoint() throws UMOException
    {
        String uri = "test://address";
        UMOEndpointFactory endpointFactory = new EndpointFactory();
        try
        {
            UMOImmutableEndpoint ep = endpointFactory.getInboundEndpoint(uri, managementContext);
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateEndpointFromGlobalEndpoint() throws UMOException
    {
        TransientRegistry r = TransientRegistry.createNew();
        r.registerObject("myGlobalEndpoint", new EndpointURIEndpointBuilder("test://address", managementContext),
            managementContext);
        String uri = "myGlobalEndpoint";
        UMOEndpointFactory endpointFactory = new EndpointFactory();
        try
        {
            UMOImmutableEndpoint ep = endpointFactory.getInboundEndpoint(uri, managementContext);
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateEndpointFromNamedConcreteEndpoint() throws UMOException
    {
        TransientRegistry r = TransientRegistry.createNew();
        r.registerObject("&myNamedConcreateEndpoint", new EndpointURIEndpointBuilder("test://address",
            managementContext), managementContext);
        String uri = "&myNamedConcreateEndpoint";
        UMOEndpointFactory endpointFactory = new EndpointFactory();
        try
        {
            UMOImmutableEndpoint ep = endpointFactory.getInboundEndpoint(uri, managementContext);
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

}
