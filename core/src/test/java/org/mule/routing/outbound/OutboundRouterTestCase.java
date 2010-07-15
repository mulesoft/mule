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

import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.tck.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

public class OutboundRouterTestCase extends AbstractMuleTestCase
{

    public void testAddGoodEndpoint() throws Exception
    {
        AbstractOutboundRouter router=new TransformerRouter();
        OutboundEndpoint endpoint=getTestOutboundEndpoint("test");
        router.addTarget(endpoint);
        assertNotNull(router.getTargets());
        assertTrue(router.getTargets().contains(endpoint));
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

    public void testSetGoodEndpoints() throws Exception
    {
        List list= new ArrayList();
        list.add(getTestOutboundEndpoint("test"));
        list.add(getTestOutboundEndpoint("test"));
        AbstractOutboundRouter router=new TransformerRouter();
        assertNotNull(router.getTargets());
        assertEquals(0, router.getTargets().size());
        router.addTarget(getTestOutboundEndpoint("test"));
        assertEquals(1, router.getTargets().size());
        router.setTargets(list);
        assertNotNull(router.getTargets());
        assertEquals(2, router.getTargets().size());
    }

    public void testSetBadEndpoints() throws Exception
    {
        List list= new ArrayList();
        list.add(getTestInboundEndpoint("test"));
        list.add(getTestOutboundEndpoint("test"));
        AbstractOutboundRouter router=new TransformerRouter();
        try{
            router.setTargets(list);
            fail("Invalid endpoint: Expecting an exception");
        }
        catch(Exception e){
            assertEquals(ClassCastException.class, e.getClass());
        }
    }
    
    public void testSetBad2Endpoints() throws Exception
    {
        List list= new ArrayList();
        list.add(getTestOutboundEndpoint("test"));
        list.add(getTestInboundEndpoint("test"));
        AbstractOutboundRouter router=new TransformerRouter();
        try{
            router.setTargets(list);
            fail("Invalid endpoint: Expecting an exception");
        }
        catch(Exception e){
            assertEquals(ClassCastException.class, e.getClass());
        }
    }

}


