/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.inbound;

import org.mule.api.endpoint.InvalidEndpointTypeException;
import org.mule.endpoint.InboundEndpoint;
import org.mule.endpoint.OutboundEndpoint;
import org.mule.tck.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

public class InboundRouterCollectionTestCase extends AbstractMuleTestCase
{

    public void testAddGoodEndpoint()
    {
        DefaultInboundRouterCollection router=new DefaultInboundRouterCollection();
        InboundEndpoint endpoint=new InboundEndpoint();
        router.addEndpoint(endpoint);
        assertNotNull(router.getEndpoints());
        assertTrue(router.getEndpoints().contains(endpoint));
    }

    public void testAddBadEndpoint2()
    {
        DefaultInboundRouterCollection router=new DefaultInboundRouterCollection();
        try{
            router.addEndpoint(new OutboundEndpoint());
            fail("Invalid endpoint: Exception exceptions");
        }
        catch(Exception e){
            assertEquals(InvalidEndpointTypeException.class, e.getClass());
        }
    }

    public void testSetGoodEndpoints()
    {
        List list= new ArrayList();
        list.add(new InboundEndpoint());
        list.add(new InboundEndpoint());
        DefaultInboundRouterCollection router=new DefaultInboundRouterCollection();
        assertNotNull(router.getEndpoints());
        assertEquals(0, router.getEndpoints().size());
        router.addEndpoint(new InboundEndpoint());
        assertEquals(1, router.getEndpoints().size());
        router.setEndpoints(list);
        assertNotNull(router.getEndpoints());
        assertEquals(2, router.getEndpoints().size());
    }

    public void testSetBadEndpoints()
    {
        List list= new ArrayList();
        list.add(new InboundEndpoint());
        list.add(new OutboundEndpoint());
        DefaultInboundRouterCollection router=new DefaultInboundRouterCollection();
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
        list.add(new InboundEndpoint());
        list.add(new OutboundEndpoint());
        DefaultInboundRouterCollection router=new DefaultInboundRouterCollection();
        try{
            router.setEndpoints(list);
            fail("Invalid endpoint: Exception exceptions");
        }
        catch(Exception e){
            assertEquals(InvalidEndpointTypeException.class, e.getClass());
        }
    }

}


