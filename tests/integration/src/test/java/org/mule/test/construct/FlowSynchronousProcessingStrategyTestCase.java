/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.construct;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;

import static org.junit.Assert.assertNull;

public class FlowSynchronousProcessingStrategyTestCase extends FlowDefaultProcessingStrategyTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/construct/flow-synchronous-processing-strategy-config.xml";
    }

    @Override
    public void testDispatchToOneWayInbound() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://oneway-in", "a", null);

        MuleMessage result = client.request("vm://oneway-out", RECEIVE_TIMEOUT);

        assertAllProcessingInRecieverThread(result);
    }

    @Override
    public void testSendToOneWayInbound() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://oneway-in", "a", null);

        assertNull(response);

        MuleMessage result = client.request("vm://oneway-out", RECEIVE_TIMEOUT);

        assertAllProcessingInClientThread(result);
    }

    @Override
    public void testDispatchToOneWayOutboundTxOnly() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://oneway-outboundtx-in", "a", null);

        MuleMessage result = client.request("vm://oneway-outboundtx-out", RECEIVE_TIMEOUT);

        assertAllProcessingInRecieverThread(result);
    }

}
