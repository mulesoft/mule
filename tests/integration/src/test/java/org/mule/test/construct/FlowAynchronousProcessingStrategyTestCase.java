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

import org.junit.Ignore;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.transaction.TransactionCoordination;

@Ignore
public class FlowAynchronousProcessingStrategyTestCase extends FlowDefaultProcessingStrategyTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/construct/flow-asynchronous-processing-strategy-config.xml";
    }

    @Override
    public void testDispatchToOneWayInboundTxOnly() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://oneway-inboundtx-in", "a", null);
        MuleMessage result = client.request("vm://dead-letter-queue", RECEIVE_TIMEOUT);
        assertNotNull(result);
    }

    @Override
    public void testDispatchToOneWayTx() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://onewaytx-in", "a", null);

        assertNull(client.request("vm://onewaytx-out", RECEIVE_TIMEOUT));

        // TODO Assert exception strategy was called

    }

    @Override
    public void testSendToOneWayTx() throws Exception
    {
        MuleClient client = muleContext.getClient();

        try
        {
            client.send("vm://oneway-tx-in", "a", null);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            TransactionCoordination.getInstance().getTransaction().rollback();
        }
    }

    @Override
    public void testSendRequestResponseInbound() throws Exception
    {
        MuleClient client = muleContext.getClient();
        try
        {
            client.send("vm://requestresponse-in", "a", null);
            fail("Exception expected");
        }
        catch (Exception e)
        {
        }
    }

    @Override
    public void testDispatchToRequestResponseInboundOneWayOutbound() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("vm://requestresponse-oneway-in", "a", null);

        // Message never gets to reciever as receiver is not polling the queue
        assertNull(client.request("vm://requestresponse-oneway-out", RECEIVE_TIMEOUT));

        // TODO Assert exception strategy was called

    }

    @Override
    public void testSendToRequestResponseInboundOneWayOutbound() throws Exception
    {
        MuleClient client = muleContext.getClient();

        try
        {
            client.send("vm://requestresponse-oneway-in", "a", null);
            fail("Exception expected");
        }
        catch (Exception e)
        {
        }
    }

    @Override
    protected void assertAllProcessingInClientThread(MuleMessage result)
    {
        assertSync(result);
        assertEquals(Thread.currentThread().getName(), result.getInboundProperty("receiver-thread"));
    }

    @Override
    protected void assertAllProcessingInRecieverThread(MuleMessage result)
    {
        assertSync(result);
        assertTrue(((String) result.getInboundProperty("receiver-thread")).startsWith("vm.receiver"));
    }

    @Override
    protected void assertSync(MuleMessage result)
    {
        assertNotNull(result);

        String receiverThread = result.getInboundProperty("receiver-thread");
        String flowThread = result.getInboundProperty("processor-thread");
        String dispatcherThread = result.getInboundProperty("dispatcher-thread");

        assertEquals(receiverThread, flowThread);
        assertEquals(flowThread, dispatcherThread);
    }

    @Override
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

    @Override
    public void testRequestResponseInboundFailingOneWayOutbound() throws Exception
    {
        MuleClient client = muleContext.getClient();

        try
        {
            client.send("vm://requestresponse-failingoneway-in", "a", null);
            fail("exception expected");
        }
        catch (Exception e)
        {

        }
    }

}
