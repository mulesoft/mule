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

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.transaction.TransactionCoordination;

public class FlowAynchronousProcessingStrategyTestCase extends FlowDefaultProcessingStrategyTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/construct/flow-asynchronous-processing-strategy-config.xml";
    }

    public void testDispatchToOneWayTxInbound() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://onewaytx-in", "a", null);

        assertNull(client.request("vm://onewaytx-out", RECEIVE_TIMEOUT));

        // TODO Assert exception strategy was called

    }

    public void testSendToOneWayTxInbound() throws Exception
    {
        MuleClient client = muleContext.getClient();

        try
        {
            client.send("vm://onewaytx-in", "a", null);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            TransactionCoordination.getInstance().getTransaction().rollback();
        }
    }

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

    public void testDispatchToRequestResponseInboundOneWayOutbound() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("vm://requestresponse-oneway-in", "a", null);

        // Message never gets to reciever as receiver is not polling the queue
        assertNull(client.request("vm://requestresponse-oneway-out", RECEIVE_TIMEOUT));

        // TODO Assert exception strategy was called

    }

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

}
