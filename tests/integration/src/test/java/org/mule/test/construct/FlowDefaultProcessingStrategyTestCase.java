/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.construct;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.MessageDispatcher;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.vm.VMMessageDispatcher;
import org.mule.transport.vm.VMMessageDispatcherFactory;

public class FlowDefaultProcessingStrategyTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/construct/flow-default-processing-strategy-config.xml";
    }

    public void testStrategyOneWayInbound() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://oneway-in", "a", null);

        MuleMessage result = client.request("vm://oneway-out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        String flowThread = result.getOutboundProperty("processor-thread");
        String outboundEndpointThread = result.getOutboundProperty("outbound-endpoint-thread");
        assertNotSame(Thread.currentThread().getName(), flowThread);
        assertNotSame(Thread.currentThread().getName(), outboundEndpointThread);
        assertNotSame(flowThread, outboundEndpointThread);
    }

    // TODO Currently fails because one-way and tx don't mix
    //    public void testStrategyOneWayTxInbound() throws Exception
    //    {
    //        MuleClient client = muleContext.getClient();
    //        client.dispatch("vm://onewaytx-in", "a", null);
    //
    //        MuleMessage result = client.request("vm://onewaytx-out", RECEIVE_TIMEOUT);
    //        assertNotNull(result);
    //        String flowThread = result.getOutboundProperty("processor-thread");
    //        String outboundEndpointThread = result.getOutboundProperty("outbound-endpoint-thread");
    //        assertTrue(flowThread.startsWith("vm.receiver"));
    //        assertEquals(flowThread, outboundEndpointThread);
    //    }

    public void testStrategyRequestResponseInbound() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://requestresponse-in", "a", null);

        assertNotNull(response);
        assertEquals(Thread.currentThread().getName(), response.getInboundProperty("processor-thread"));
    }

    public void testStrategyRequestResponseInboundOneWayOutbound() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://requestresponse-oneway-in", "a", null);
        assertNull(response);

        MuleMessage result = client.request("vm://requestresponse-oneway-out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        String flowThread = result.getOutboundProperty("processor-thread");
        String outboundEndpointThread = result.getOutboundProperty("outbound-endpoint-thread");
        assertEquals(Thread.currentThread().getName(), flowThread);
        assertEquals(Thread.currentThread().getName(), outboundEndpointThread);
        assertEquals(flowThread, outboundEndpointThread);
    }

    public void testStrategyRequestResponseInboundFailingOneWayOutbound() throws Exception
    {
        MuleClient client = muleContext.getClient();

        try
        {
            MuleMessage response = client.send("vm://requestresponse-failingoneway-in", "a", null);
            fail("exception expected");
        }
        catch (Exception e)
        {

        }
    }

    // public void testRequestResponse() throws Exception
    // {
    // }
    //
    // public void testSynchronous() throws Exception
    // {
    // assertEquals(SynchronousProcessingStrategy.class,
    // getFlowProcesingStrategy("synchronousFlow").getClass());
    // }
    //
    // public void testAsynchronous() throws Exception
    // {
    // assertEquals(AsynchronousProcessingStrategy.class,
    // getFlowProcesingStrategy("asynchronousFlow").getClass());
    // }
    //
    // public void testQueuedAsynchronous() throws Exception
    // {
    // assertEquals(QueuedAsynchronousProcessingStrategy.class,
    // getFlowProcesingStrategy("queuedAsynchronousFlow").getClass());
    // }

    public static class ThreadSensingMessageProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            event.getMessage().setOutboundProperty("processor-thread", Thread.currentThread().getName());
            return event;
        }
    }

    public static class ThreadSensingVMMessageDispatcherFactory extends VMMessageDispatcherFactory
    {

        @Override
        public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
        {
            return new ThreadSensingVMMessageDispatcher(endpoint);
        }
    }

    public static class ThreadSensingVMMessageDispatcher extends VMMessageDispatcher
    {
        public ThreadSensingVMMessageDispatcher(OutboundEndpoint endpoint)
        {
            super(endpoint);
        }

        @Override
        protected void doDispatch(MuleEvent event) throws Exception
        {
            event.getMessage().setOutboundProperty("outbound-endpoint-thread",
                Thread.currentThread().getName());
            super.doDispatch(event);
        }
    }

}
