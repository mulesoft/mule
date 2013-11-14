/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;

public class FlowSynchronousProcessingStrategyTestCase extends FlowDefaultProcessingStrategyTestCase
{
    @Override
    protected String getConfigFile()
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
