/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.endpoint.InvalidEndpointTypeException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.routing.outbound.AbstractOutboundRouter;
import org.mule.routing.outbound.TransformerRouter;
import org.mule.tck.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

public class AbstractExceptionListenerTestCase extends AbstractMuleTestCase
{

    public void testAddGoodEndpoint() throws Exception
    {
        AbstractOutboundRouter router=new TransformerRouter();
        OutboundEndpoint endpoint= getTestOutboundEndpoint("test");
        router.addEndpoint(endpoint);
        assertNotNull(router.getEndpoints());
        assertTrue(router.getEndpoints().contains(endpoint));    }

//    public void testAddBadEndpoint()
//    {
//        AbstractOutboundRouter router=new TransformerRouter();
//        try{
//            router.addEndpoint(new InboundEndpoint());
//            fail("Invalid endpoint: Exception expected");
//        }
//        catch(Exception e){
//            assertEquals(InvalidEndpointTypeException.class, e.getClass());
//        }
//    }
//
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

    public void testSetGoodEndpoints() throws Exception
    {
        List list= new ArrayList();
        list.add(getTestOutboundEndpoint("test"));
        list.add(getTestOutboundEndpoint("test"));
        AbstractOutboundRouter router=new TransformerRouter();
        assertNotNull(router.getEndpoints());
        assertEquals(0, router.getEndpoints().size());
        router.addEndpoint(getTestOutboundEndpoint("test"));
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
        AbstractOutboundRouter router=new TransformerRouter();
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
