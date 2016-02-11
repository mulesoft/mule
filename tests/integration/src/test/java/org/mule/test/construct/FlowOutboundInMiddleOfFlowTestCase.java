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
import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Test;

public class FlowOutboundInMiddleOfFlowTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/construct/flow-outbound-in-middle-of-flow.xml";
    }

    @Test
    public void testOutboundInMiddleOfFlow() throws Exception
    {
        MuleClient client = muleContext.getClient();
        
        flowRunner("flowTest").withPayload("message").asynchronously().run();
        
        MuleMessage msg = client.request("test://test.out.1", 1000);
        assertEquals("messagehello", getPayloadAsString(msg));
        
        MuleMessage msg2 = client.request("test://test.out.2", RECEIVE_TIMEOUT);
        assertEquals("messagebye", getPayloadAsString(msg2));
        
        MuleMessage msg3 = client.request("test://test.out.3", RECEIVE_TIMEOUT);
        assertEquals("egassem", getPayloadAsString(msg3));
    }
}


