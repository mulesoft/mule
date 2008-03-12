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

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.InvalidEndpointTypeException;
import org.mule.tck.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

public class ResponseRouterCollectionTestCase extends AbstractMuleTestCase
{

    public void testAddGoodEndpoint() throws Exception
    {
        DefaultResponseRouterCollection router=new DefaultResponseRouterCollection();
        InboundEndpoint endpoint=getTestInboundEndpoint("test");
        router.addEndpoint(endpoint);
        assertNotNull(router.getEndpoints());
        assertTrue(router.getEndpoints().contains(endpoint));
    }

    public void testSetGoodEndpoints() throws Exception
    {
        List list= new ArrayList();
        list.add(getTestInboundEndpoint("test"));
        list.add(getTestInboundEndpoint("test"));
        DefaultResponseRouterCollection router=new DefaultResponseRouterCollection();
        assertNotNull(router.getEndpoints());
        assertEquals(0, router.getEndpoints().size());
        router.addEndpoint(getTestInboundEndpoint("test"));
        assertEquals(1, router.getEndpoints().size());
        router.setEndpoints(list);
        assertNotNull(router.getEndpoints());
        assertEquals(2, router.getEndpoints().size());
    }

    public void testSetBadEndpoints() throws Exception
    {
        List list= new ArrayList();
        list.add(getTestInboundEndpoint("test"));
        list.add(getTestOutboundEndpoint("test"));
        DefaultResponseRouterCollection router=new DefaultResponseRouterCollection();
        try{
            router.setEndpoints(list);
            fail("Invalid endpoint: Exception exceptions");
        }
        catch(Exception e){
            assertEquals(InvalidEndpointTypeException.class, e.getClass());
        }
    }
    
    public void testSetBad2Endpoints() throws Exception
    {
        List list= new ArrayList();
        list.add(getTestInboundEndpoint("test"));
        list.add(getTestOutboundEndpoint("test"));
        DefaultResponseRouterCollection router=new DefaultResponseRouterCollection();
        try{
            router.setEndpoints(list);
            fail("Invalid endpoint: Exception exceptions");
        }
        catch(Exception e){
            assertEquals(InvalidEndpointTypeException.class, e.getClass());
        }
    }

}


