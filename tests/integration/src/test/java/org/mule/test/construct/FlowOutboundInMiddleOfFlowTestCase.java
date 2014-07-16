/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Ignore;
import org.junit.Test;

public class FlowOutboundInMiddleOfFlowTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/construct/flow-outbound-in-middle-of-flow.xml";
    }

    @Test
    @Ignore("MULE-6926: flaky test")
    public void testOutboundInMiddleOfFlow() throws Exception
    {
        MuleClient client = muleContext.getClient();
        
        client.dispatch("vm://test.in", "message", null);
        
        MuleMessage msg = client.request("vm://test.out.1", 1000);
        assertEquals("messagehello", msg.getPayloadAsString());
        
        MuleMessage msg2 = client.request("vm://test.out.2", 5000);
        assertEquals("messagebye", msg2.getPayloadAsString());
        
        MuleMessage msg3 = client.request("vm://test.out.3", 5000);
        assertEquals("egassem", msg3.getPayloadAsString());
    }
}


