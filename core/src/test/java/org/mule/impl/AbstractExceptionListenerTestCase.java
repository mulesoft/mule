/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.impl.endpoint.InboundEndpoint;
import org.mule.impl.endpoint.OutboundEndpoint;
import org.mule.impl.endpoint.OutboundStreamingEndpoint;
import org.mule.routing.outbound.AbstractOutboundRouter;
import org.mule.routing.outbound.TransformerRouter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.InvalidEndpointTypeException;

import java.util.ArrayList;
import java.util.List;

public class AbstractExceptionListenerTestCase extends AbstractMuleTestCase
{

    public void testAddGoodEndpoint()
    {
        AbstractOutboundRouter router=new TransformerRouter();
        OutboundEndpoint endpoint=new OutboundEndpoint();
        router.addEndpoint(endpoint);
        assertNotNull(router.getEndpoints());
        assertTrue(router.getEndpoints().contains(endpoint));    }

    public void testAddBadEndpoint()
    {
        AbstractOutboundRouter router=new TransformerRouter();
        try{
            router.addEndpoint(new InboundEndpoint());
            fail("Invalid endpoint: Exception expected");
        }
        catch(Exception e){
            assertEquals(InvalidEndpointTypeException.class, e.getClass());
        }
    }

    public void testAddBadEndpoint2()
    {
        AbstractOutboundRouter router=new TransformerRouter();
        try{
            router.addEndpoint(new InboundEndpoint());
            fail("Invalid endpoint: Exception exceptions");
        }
        catch(Exception e){
            assertEquals(InvalidEndpointTypeException.class, e.getClass());
        }
    }

    public void testSetGoodEndpoints()
    {
        List list= new ArrayList();
        list.add(new OutboundEndpoint());
        list.add(new OutboundStreamingEndpoint());
        list.add(new OutboundEndpoint());
        AbstractOutboundRouter router=new TransformerRouter();
        assertNotNull(router.getEndpoints());
        assertEquals(0, router.getEndpoints().size());
        router.addEndpoint(new OutboundEndpoint());
        assertEquals(1, router.getEndpoints().size());
        router.setEndpoints(list);
        assertNotNull(router.getEndpoints());
        assertEquals(3, router.getEndpoints().size());
    }

    public void testSetBadEndpoints()
    {
        List list= new ArrayList();
        list.add(new InboundEndpoint());
        list.add(new OutboundEndpoint());
        list.add(new OutboundStreamingEndpoint());
        AbstractOutboundRouter router=new TransformerRouter();
        try{
            router.setEndpoints(list);
            fail("Invalid endpoint: Exception exceptions");
        }
        catch(Exception e){
            assertEquals(InvalidEndpointTypeException.class, e.getClass());
        }
    }
    
    public void testSetBad2Endpoints()
    {
        List list= new ArrayList();
        list.add(new OutboundStreamingEndpoint());
        list.add(new InboundEndpoint());
        list.add(new OutboundEndpoint());
        AbstractOutboundRouter router=new TransformerRouter();
        try{
            router.setEndpoints(list);
            fail("Invalid endpoint: Exception exceptions");
        }
        catch(Exception e){
            assertEquals(InvalidEndpointTypeException.class, e.getClass());
        }
    }


}


