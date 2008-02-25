/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.registry.Registry;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestConnector;

public class EndpointFactoryTestCase extends AbstractMuleTestCase
{

    public void testCreateInboundEndpoint() throws MuleException
    {
        String uri = "test://address";
        EndpointFactory endpointFactory = new DefaultEndpointFactory();
        endpointFactory.setMuleContext(muleContext);
        try
        {
            ImmutableEndpoint ep = endpointFactory.getInboundEndpoint(uri);
            assertEquals(InboundEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertTrue(ep.isInbound());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateInboundEndpointFromGlobalEndpoint() throws MuleException
    {
        muleContext.getRegistry().registerEndpointBuilder("myGlobalEndpoint",
            new EndpointURIEndpointBuilder("test://address", muleContext));
        String uri = "myGlobalEndpoint";
        EndpointFactory endpointFactory = new DefaultEndpointFactory();
        endpointFactory.setMuleContext(muleContext);
        try
        {
            ImmutableEndpoint ep = endpointFactory.getInboundEndpoint(uri);
            assertEquals(InboundEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertTrue(ep.isInbound());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateInboundEndpointFromNamedConcreteEndpoint() throws MuleException
    {
        muleContext.getRegistry().registerEndpointBuilder("&myNamedConcreateEndpoint",
            new EndpointURIEndpointBuilder("test://address", muleContext));
        String uri = "&myNamedConcreateEndpoint";
        EndpointFactory endpointFactory = new DefaultEndpointFactory();
        endpointFactory.setMuleContext(muleContext);
        try
        {
            ImmutableEndpoint ep = endpointFactory.getInboundEndpoint(uri);
            assertEquals(InboundEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertTrue(ep.isInbound());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateOutboundEndpoint() throws MuleException
    {
        String uri = "test://address";
        EndpointFactory endpointFactory = new DefaultEndpointFactory();
        endpointFactory.setMuleContext(muleContext);
        try
        {
            ImmutableEndpoint ep = endpointFactory.getOutboundEndpoint(uri);
            assertEquals(OutboundEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertTrue(ep.isOutbound());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateoutboundEndpointFromGlobalEndpoint() throws MuleException
    {
        muleContext.getRegistry().registerEndpointBuilder("myGlobalEndpoint",
            new EndpointURIEndpointBuilder("test://address", muleContext));
        String uri = "myGlobalEndpoint";
        EndpointFactory endpointFactory = new DefaultEndpointFactory();
        endpointFactory.setMuleContext(muleContext);
        try
        {
            ImmutableEndpoint ep = endpointFactory.getOutboundEndpoint(uri);
            assertEquals(OutboundEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertTrue(ep.isOutbound());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateoutboundEndpointFromNamedConcreteEndpoint() throws MuleException
    {
        muleContext.getRegistry().registerEndpointBuilder("&myNamedConcreateEndpoint",
            new EndpointURIEndpointBuilder("test://address", muleContext));
        String uri = "&myNamedConcreateEndpoint";
        EndpointFactory endpointFactory = new DefaultEndpointFactory();
        endpointFactory.setMuleContext(muleContext);
        try
        {
            ImmutableEndpoint ep = endpointFactory.getOutboundEndpoint(uri);
            assertEquals(OutboundEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertTrue(ep.isOutbound());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateInboundEndpointWithBuilder() throws MuleException
    {
        EndpointBuilder builder = new EndpointURIEndpointBuilder("test://address", muleContext);
        EndpointFactory endpointFactory = new DefaultEndpointFactory();
        endpointFactory.setMuleContext(muleContext);
        try
        {
            ImmutableEndpoint ep = endpointFactory.getInboundEndpoint(builder);
            assertEquals(InboundEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertTrue(ep.isInbound());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateOutboundEndpointWithBuilder() throws MuleException
    {
        EndpointBuilder builder = new EndpointURIEndpointBuilder("test://address", muleContext);
        EndpointFactory endpointFactory = new DefaultEndpointFactory();
        endpointFactory.setMuleContext(muleContext);
        try
        {
            ImmutableEndpoint ep = endpointFactory.getOutboundEndpoint(builder);
            assertEquals(OutboundEndpoint.class, ep.getClass());
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertTrue(ep.isOutbound());
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateEndpoint() throws MuleException
    {
        String uri = "test://address";
        EndpointFactory endpointFactory = new DefaultEndpointFactory();
        endpointFactory.setMuleContext(muleContext);
        try
        {
            ImmutableEndpoint ep = endpointFactory.getInboundEndpoint(uri);
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateEndpointFromGlobalEndpoint() throws MuleException
    {
        Registry r = muleContext.getRegistry();        
        r.registerObject("myGlobalEndpoint", new EndpointURIEndpointBuilder("test://address", muleContext),
            muleContext);
        String uri = "myGlobalEndpoint";
        EndpointFactory endpointFactory = new DefaultEndpointFactory();
        endpointFactory.setMuleContext(muleContext);
        try
        {
            ImmutableEndpoint ep = endpointFactory.getInboundEndpoint(uri);
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCreateEndpointFromNamedConcreteEndpoint() throws MuleException
    {
        Registry r = muleContext.getRegistry();
        r.registerObject("&myNamedConcreateEndpoint", new EndpointURIEndpointBuilder("test://address", muleContext));
        String uri = "&myNamedConcreateEndpoint";
        EndpointFactory endpointFactory = new DefaultEndpointFactory();
        endpointFactory.setMuleContext(muleContext);
        try
        {
            ImmutableEndpoint ep = endpointFactory.getInboundEndpoint(uri);
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
        }
        catch (Exception e)
        {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
    
    public void testCreateEndpointByCustomizingEndpointBuilder() throws MuleException
    {
        // Create and register two connectors
        TestConnector testConnector1 = new TestConnector();
        testConnector1.setName("testConnector1");
        TestConnector testConnector2 = new TestConnector();
        testConnector2.setName("testConnector2");
        muleContext.getRegistry().registerConnector(testConnector1);
        muleContext.getRegistry().registerConnector(testConnector2);
        
        String globalEndpointName = "concreteEndpoint";
        
        // Create and register a endpoint builder (global endpoint) with connector1
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("test://address", muleContext);
        endpointBuilder.setConnector(testConnector1);
        muleContext.getRegistry().registerObject(globalEndpointName, endpointBuilder);
        EndpointFactory endpointFactory = new DefaultEndpointFactory();
        endpointFactory.setMuleContext(muleContext);
        try
        {
            // Test that DefaultEndpointFactory.getEndpointBuilder() returns a new EndpointBuilder instance equal to
            // the one we registered earlier
            EndpointBuilder endpointBuilder1 = endpointFactory.getEndpointBuilder(globalEndpointName);
            assertNotSame(endpointBuilder1, endpointBuilder);
            assertTrue(endpointBuilder1.equals(endpointBuilder));

            // Test that DefaultEndpointFactory.getEndpointBuilder() returns a new EndpointBuilder instance equal to
            // the one we registered earlier
            EndpointBuilder endpointBuilder2 = endpointFactory.getEndpointBuilder(globalEndpointName);
            assertNotSame(endpointBuilder2, endpointBuilder);
            assertTrue(endpointBuilder2.equals(endpointBuilder));

            // Check that all EndpointBuilder's returned are unique but equal
            assertNotSame(endpointBuilder1, endpointBuilder2);
            assertTrue(endpointBuilder1.equals(endpointBuilder2));
            assertEquals(endpointBuilder1.hashCode(), endpointBuilder2.hashCode());

            // Test creating an endpoint from endpointBuilder1
            endpointBuilder1.setSynchronous(true);
            endpointBuilder1.setRemoteSyncTimeout(99);
            ImmutableEndpoint ep = endpointFactory.getInboundEndpoint(endpointBuilder1);
            assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
            assertTrue(ep.isSynchronous());
            assertEquals(99, ep.getRemoteSyncTimeout());
            assertNotNull(ep.getConnector());
            assertEquals(testConnector1, ep.getConnector());

            // Test creating an endpoint from endpointBuilder2
            endpointBuilder2.setSynchronous(false);
            endpointBuilder2.setRemoteSyncTimeout(0);
            endpointBuilder2.setConnector(testConnector2);
            ImmutableEndpoint ep2 = endpointFactory.getInboundEndpoint(endpointBuilder2);
            assertEquals(ep2.getEndpointURI().getUri().toString(), "test://address");
            assertFalse(ep2.isSynchronous());
            assertEquals(0, ep2.getRemoteSyncTimeout());
            assertNotNull(ep.getConnector());
            assertEquals(testConnector2, ep2.getConnector());

            // Test creating a new endpoint from endpointBuilder1
            ImmutableEndpoint ep3 = endpointFactory.getInboundEndpoint(endpointBuilder1);
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
