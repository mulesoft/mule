/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.api.endpoint.InvalidEndpointTypeException;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.endpoint.DefaultOutboundEndpoint;
import org.mule.tck.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

public class AbstractOutboundRouterTestCase extends AbstractMuleTestCase
{

    public void testAddGoodEndpoint()
    {
        AbstractOutboundRouter router=new TransformerRouter();
        DefaultOutboundEndpoint endpoint=new DefaultOutboundEndpoint();
        router.addEndpoint(endpoint);
        assertNotNull(router.getEndpoints());
        assertTrue(router.getEndpoints().contains(endpoint));
    }

//    public void testAddBadEndpoint2()
//    {
//        AbstractOutboundRouter router=new TransformerRouter();
//        try{
//            router.addEndpoint(new InboundEndpoint());
//            fail("Invalid endpoint: Exception exceptions");
//        }
//        catch(Exception e){
//            assertEquals(InvalidEndpointTypeException.class, e.getClass());
//        }
//    }

    public void testSetGoodEndpoints()
    {
        List list= new ArrayList();
        list.add(new DefaultOutboundEndpoint());
        list.add(new DefaultOutboundEndpoint());
        AbstractOutboundRouter router=new TransformerRouter();
        assertNotNull(router.getEndpoints());
        assertEquals(0, router.getEndpoints().size());
        router.addEndpoint(new DefaultOutboundEndpoint());
        assertEquals(1, router.getEndpoints().size());
        router.setEndpoints(list);
        assertNotNull(router.getEndpoints());
        assertEquals(2, router.getEndpoints().size());
    }

    public void testSetBadEndpoints()
    {
        List list= new ArrayList();
        list.add(new DefaultInboundEndpoint());
        list.add(new DefaultOutboundEndpoint());
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
        list.add(new DefaultOutboundEndpoint());
        list.add(new DefaultInboundEndpoint());
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


