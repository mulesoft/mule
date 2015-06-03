/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.VoidMuleEvent;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageDispatcher;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.vm.VMMessageDispatcher;
import org.mule.transport.vm.VMMessageDispatcherFactory;
import org.mule.transport.vm.VMMessageReceiver;

import org.junit.Test;

public class FlowDefaultProcessingStrategyTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/construct/flow-default-processing-strategy-config.xml";
    }

    @Test
    public void testDispatchToOneWayInbound() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://oneway-in", "a", null);

        MuleMessage result = client.request("vm://oneway-out", RECEIVE_TIMEOUT);

        assertAllProcessingAsync(result);
    }

    @Test
    public void testSendToOneWayInbound() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://oneway-in", "a", null);

        assertNull(response);

        MuleMessage result = client.request("vm://oneway-out", RECEIVE_TIMEOUT);

        assertNotNull(result);

        String receiverThread = result.getInboundProperty("receiver-thread");
        String flowThread = result.getInboundProperty("processor-thread");
        String dispatcherThread = result.getInboundProperty("dispatcher-thread");

        assertEquals(Thread.currentThread().getName(), receiverThread);
        assertFalse(receiverThread.equals(flowThread));
        assertFalse(flowThread.equals(dispatcherThread));
    }

    @Test
    public void testDispatchToOneWayTx() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://oneway-tx-in", "a", null);

        MuleMessage result = client.request("vm://oneway-tx-out", RECEIVE_TIMEOUT);

        assertAllProcessingInRecieverThread(result);
    }

    @Test
    public void testSendToOneWayTx() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://oneway-tx-in", "a", null);

        assertNull(response);

        MuleMessage result = client.request("vm://oneway-tx-out", RECEIVE_TIMEOUT);
        assertAllProcessingInClientThread(result);
    }


    @Test
    public void testDispatchToOneWayInboundTxOnly() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://oneway-inboundtx-in", "a", null);

        MuleMessage result = client.request("vm://oneway-inboundtx-out", RECEIVE_TIMEOUT);

        assertAllProcessingInRecieverThread(result);
    }

    @Test
    public void testDispatchToOneWayOutboundTxOnly() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://oneway-outboundtx-in", "a", null);

        MuleMessage result = client.request("vm://oneway-outboundtx-out", RECEIVE_TIMEOUT);

        assertAllProcessingAsync(result);
    }

    @Test
    public void testSendRequestResponseInbound() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://requestresponse-in", "a", null);

        assertAllProcessingInClientThread(response);
    }

    @Test
    public void testDispatchToRequestResponseInboundOneWayOutbound() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("vm://requestresponse-oneway-in", "a", null);

        // Message never gets to reciever as receiver is not polling the queue
        assertNull(client.request("vm://requestresponse-oneway-out", RECEIVE_TIMEOUT));
    }

    @Test
    public void testSendToRequestResponseInboundOneWayOutbound() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://requestresponse-oneway-in", "a", null);
        assertEquals("a", response.getPayload());

        MuleMessage result = client.request("vm://requestresponse-oneway-out", RECEIVE_TIMEOUT);

        assertAllProcessingInClientThread(result);
    }

    protected void assertAllProcessingInClientThread(MuleMessage result)
    {
        assertSync(result);
        assertEquals(Thread.currentThread().getName(), result.getInboundProperty("receiver-thread"));
    }

    protected void assertAllProcessingInRecieverThread(MuleMessage result)
    {
        assertSync(result);
        assertTrue(((String) result.getInboundProperty("receiver-thread")).startsWith("vm.receiver"));
    }

    protected void assertSync(MuleMessage result)
    {
        assertNotNull(result);

        String receiverThread = result.getInboundProperty("receiver-thread");
        String flowThread = result.getInboundProperty("processor-thread");
        String dispatcherThread = result.getInboundProperty("dispatcher-thread");

        assertEquals(receiverThread, flowThread);
        assertEquals(flowThread, dispatcherThread);
    }

    protected void assertAllProcessingAsync(MuleMessage result)
    {
        assertNotNull(result);

        String receiverThread = result.getInboundProperty("receiver-thread");
        String flowThread = result.getInboundProperty("processor-thread");
        String dispatcherThread = result.getInboundProperty("dispatcher-thread");

        assertTrue(receiverThread.startsWith("vm.receiver"));
        assertFalse(receiverThread.equals(flowThread));
        assertFalse(flowThread.equals(dispatcherThread));
        assertFalse(receiverThread.equals(dispatcherThread));
    }

    @Test
    public void testRequestResponseInboundFailingOneWayOutbound() throws Exception
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
            event.getMessage().setOutboundProperty("dispatcher-thread", Thread.currentThread().getName());
            super.doDispatch(event);
        }

        @Override
        protected MuleMessage doSend(MuleEvent event) throws Exception
        {
            event.getMessage().setOutboundProperty("dispatcher-thread", Thread.currentThread().getName());
            return super.doSend(event);
        }
    }

    public static class ThreadSensingVMMessageReceiver extends VMMessageReceiver
    {

        public ThreadSensingVMMessageReceiver(Connector connector,
                                              FlowConstruct flowConstruct,
                                              InboundEndpoint endpoint) throws CreateException
        {
            super(connector, flowConstruct, endpoint);
        }

        @Override
        public MuleMessage onCall(MuleMessage message) throws MuleException
        {
            try
            {
                message.setOutboundProperty("receiver-thread", Thread.currentThread().getName());
                MuleEvent event = routeMessage(message);
                MuleMessage returnedMessage = (!endpoint.getExchangePattern().hasResponse() || event == null || VoidMuleEvent.getInstance().equals(event))
                                                                                                          ? null
                                                                                                          : event.getMessage();
                /**
                 * if (returnedMessage != null) { returnedMessage = returnedMessage.createInboundMessage(); }
                 **/
                return returnedMessage;
            }
            catch (Exception e)
            {
                throw new DefaultMuleException(e);
            }
        }

        @Override
        protected MuleEvent processMessage(Object msg) throws Exception
        {
            MuleMessage message = (MuleMessage) msg;

            // Rewrite the message to treat it as a new message
            MuleMessage newMessage = message.createInboundMessage();
            newMessage.setOutboundProperty("receiver-thread", Thread.currentThread().getName());
            return routeMessage(newMessage);
        }

    }

}
