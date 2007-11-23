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

import org.mule.registry.Registry;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestConnector;
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
        Registry r = managementContext.getRegistry();        
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
        Registry r = managementContext.getRegistry();
        r.registerObject("myNamedConcreateEndpoint", r.lookupEndpointFactory().getInboundEndpoint("test://test",
            managementContext), managementContext);
        r.registerObject("&myNamedConcreateEndpoint", new EndpointURIEndpointBuilder("test://address",
            managementContext), managementContext);
        String uri = "myNamedConcreateEndpoint";
        UMOEndpointFactory endpointFactory = new EndpointFactory();
        try
        {
            UMOImmutableEndpoint ep = endpointFactory.getInboundEndpoint(uri, managementContext);
            fail("Expected exception.  Cannot create endpoints from named conrete endpoints.");
        }
        catch (Exception e)
        {
        }
    }
    
    public void testCreateEndpointByCustomizingEndpointBuilder() throws UMOException
    {
        Registry r = managementContext.getRegistry();
        
        // Create and register two connectors
        TestConnector testConnector1 = new TestConnector();
        testConnector1.setName("testConnector1");
        TestConnector testConnector2 = new TestConnector();
        testConnector2.setName("testConnector2");
        r.registerConnector(testConnector1, managementContext);
        r.registerConnector(testConnector2, managementContext);
        
        String globalEndpointName = "concreteEndpoint";
        
        // Create and register a endpoint builder (global endpoint) with connector1
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("test://address", managementContext);
        endpointBuilder.setConnector(testConnector1);
        r.registerObject(globalEndpointName, endpointBuilder, managementContext);
        UMOEndpointFactory endpointFactory = new EndpointFactory();
        try
        {
            // Test that EndpointFactory.getEndpointBuilder() returns a new EndpointBuilder instance equal to
            // the one we registered earlier
            UMOEndpointBuilder endpointBuilder1 = endpointFactory.getEndpointBuilder(globalEndpointName,
                managementContext);
            assertNotSame(endpointBuilder1, endpointBuilder);
            assertTrue(endpointBuilder1.equals(endpointBuilder));

            // Test that EndpointFactory.getEndpointBuilder() returns a new EndpointBuilder instance equal to
            // the one we registered earlier
            UMOEndpointBuilder endpointBuilder2 = endpointFactory.getEndpointBuilder(globalEndpointName,
                managementContext);
            assertNotSame(endpointBuilder2, endpointBuilder);
            assertTrue(endpointBuilder2.equals(endpointBuilder));

            // Check that all EndpointBuilder's returned are unique but equal
            assertNotSame(endpointBuilder1, endpointBuilder2);
            assertTrue(endpointBuilder1.equals(endpointBuilder2));
            assertEquals(endpointBuilder1.hashCode(), endpointBuilder2.hashCode());

            // Test creating an endpoint from endpointBuilder1
            endpointBuilder1.setSynchronous(true);
            endpointBuilder1.setRemoteSyncTimeout(99);
            UMOImmutableEndpoint ep = endpointFactory.getInboundEndpoint(endpointBuilder1, managementContext);
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertTrue(ep.isSynchronous());
            assertEquals(99, ep.getRemoteSyncTimeout());
            assertNotNull(ep.getConnector());
            assertEquals(testConnector1, ep.getConnector());

            // Test creating an endpoint from endpointBuilder2
            endpointBuilder2.setSynchronous(false);
            endpointBuilder2.setRemoteSyncTimeout(0);
            endpointBuilder2.setConnector(testConnector2);
            UMOImmutableEndpoint ep2 = endpointFactory.getInboundEndpoint(endpointBuilder2, managementContext);
            assertEquals(ep2.getEndpointURI().getUri().toString(), "test://address");
            assertFalse(ep2.isSynchronous());
            assertEquals(0, ep2.getRemoteSyncTimeout());
            assertNotNull(ep.getConnector());
            assertEquals(testConnector2, ep2.getConnector());

            // Test creating a new endpoint from endpointBuilder1
            UMOImmutableEndpoint ep3 = endpointFactory.getInboundEndpoint(endpointBuilder1, managementContext);
            assertEquals(ep3.getEndpointURI().getUri().toString(), "test://address");
            assertTrue(ep3.getRemoteSyncTimeout() != 0);
            assertTrue(ep3.isSynchronous());
            assertNotNull(ep.getConnector());
            assertEquals(testConnector1, ep3.getConnector());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
    
}
