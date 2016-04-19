/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class OutboundRouterTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testAddGoodEndpoint() throws Exception
    {
        AbstractOutboundRouter router = new DummyOutboundRouter();
        OutboundEndpoint endpoint = getTestOutboundEndpoint("test");
        router.addRoute(endpoint);
        assertNotNull(router.getRoutes());
        assertTrue(router.getRoutes().contains(endpoint));
    }

    @Test
    public void testAddGoodProcessor() throws Exception
    {
        AbstractOutboundRouter router = new DummyOutboundRouter();
        MessageProcessor processor = getTestMessageProcessor();
        router.addRoute(processor);
        assertNotNull(router.getRoutes());
        assertTrue(router.getRoutes().contains(processor));
    }

    private MessageProcessor getTestMessageProcessor()
    {
        return mock(MessageProcessor.class);
    }

    @Test
    public void testSetGoodEndpoints() throws Exception
    {
        List<MessageProcessor> list= new ArrayList<MessageProcessor>();
        list.add(getTestOutboundEndpoint("test"));
        list.add(getTestOutboundEndpoint("test"));
        AbstractOutboundRouter router = new DummyOutboundRouter();
        assertNotNull(router.getRoutes());
        assertEquals(0, router.getRoutes().size());
        router.addRoute(getTestOutboundEndpoint("test"));
        assertEquals(1, router.getRoutes().size());
        router.setRoutes(list);
        assertNotNull(router.getRoutes());
        assertEquals(2, router.getRoutes().size());
    }

    @Test
    public void testSetGoodProcessors() throws Exception
    {
        List<MessageProcessor> list = new ArrayList<MessageProcessor>();
        list.add(getTestMessageProcessor());
        list.add(getTestMessageProcessor());
        AbstractOutboundRouter router = new DummyOutboundRouter();
        assertNotNull(router.getRoutes());
        assertEquals(0, router.getRoutes().size());
        router.addRoute(getTestMessageProcessor());
        assertEquals(1, router.getRoutes().size());
        router.setRoutes(list);
        assertNotNull(router.getRoutes());
        assertEquals(2, router.getRoutes().size());
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testSetBadEndpoints() throws Exception
    {
        List list = new ArrayList();
        list.add(getTestInboundEndpoint("test"));
        list.add(getTestOutboundEndpoint("test"));
        AbstractOutboundRouter router = new DummyOutboundRouter();

        try
        {
            router.setRoutes(list);
            fail("Invalid endpoint: Expecting an exception");
        }
        catch (Exception e)
        {
            assertEquals(ClassCastException.class, e.getClass());
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testSetBadProcessors() throws Exception
    {
        List list = new ArrayList();
        list.add(mock(MessageSource.class));
        list.add(getTestMessageProcessor());
        AbstractOutboundRouter router = new DummyOutboundRouter();

        try
        {
            router.setRoutes(list);
            fail("Invalid endpoint: Expecting an exception");
        }
        catch(Exception e)
        {
            assertEquals(ClassCastException.class, e.getClass());
        }
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testSetBad2Endpoints() throws Exception
    {
        List list= new ArrayList();
        list.add(getTestOutboundEndpoint("test"));
        list.add(getTestInboundEndpoint("test"));
        AbstractOutboundRouter router = new DummyOutboundRouter();

        try
        {
            router.setRoutes(list);
            fail("Invalid endpoint: Expecting an exception");
        }
        catch (Exception e)
        {
            assertEquals(ClassCastException.class, e.getClass());
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testSetBad2Processors() throws Exception
    {
        List list = new ArrayList();
        list.add(getTestMessageProcessor());
        list.add(mock(MessageSource.class));
        AbstractOutboundRouter router = new DummyOutboundRouter();

        try
        {
            router.setRoutes(list);
            fail("Invalid endpoint: Expecting an exception");
        }
        catch(Exception e)
        {
            assertEquals(ClassCastException.class, e.getClass());
        }
    }

    private static class DummyOutboundRouter extends AbstractOutboundRouter
    {
        @Override
        public boolean isMatch(MuleMessage message) throws MuleException
        {
            return false;
        }

        @Override
        protected MuleEvent route(MuleEvent event) throws MessagingException
        {
            return null;
        }
    }
}
