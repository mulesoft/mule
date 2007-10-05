/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.response;

import org.mule.impl.endpoint.InboundEndpoint;
import org.mule.impl.endpoint.InboundStreamingEndpoint;
import org.mule.impl.endpoint.OutboundEndpoint;
import org.mule.impl.endpoint.ResponseEndpoint;
import org.mule.impl.endpoint.ResponseStreamingEndpoint;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.InvalidEndpointTypeException;

import java.util.ArrayList;
import java.util.List;

public class ResponseRouterCollectionTestCase extends AbstractMuleTestCase
{

    public void testAddGoodEndpoint()
    {
        ResponseRouterCollection router=new ResponseRouterCollection();
        ResponseEndpoint endpoint=new ResponseEndpoint();
        router.addEndpoint(endpoint);
        assertNotNull(router.getEndpoints());
        assertTrue(router.getEndpoints().contains(endpoint));
    }

    public void testAddBadEndpoint2()
    {
        ResponseRouterCollection router=new ResponseRouterCollection();
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
        list.add(new ResponseEndpoint());
        list.add(new ResponseStreamingEndpoint());
        list.add(new ResponseEndpoint());
        ResponseRouterCollection router=new ResponseRouterCollection();
        assertNotNull(router.getEndpoints());
        assertEquals(0, router.getEndpoints().size());
        router.addEndpoint(new ResponseEndpoint());
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
        list.add(new InboundStreamingEndpoint());
        ResponseRouterCollection router=new ResponseRouterCollection();
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
        list.add(new InboundStreamingEndpoint());
        list.add(new OutboundEndpoint());
        ResponseRouterCollection router=new ResponseRouterCollection();
        try{
            router.setEndpoints(list);
            fail("Invalid endpoint: Exception exceptions");
        }
        catch(Exception e){
            assertEquals(InvalidEndpointTypeException.class, e.getClass());
        }
    }

}


