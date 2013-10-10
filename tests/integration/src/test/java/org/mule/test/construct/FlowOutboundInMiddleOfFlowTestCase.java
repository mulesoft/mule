/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.construct;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

public class FlowOutboundInMiddleOfFlowTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/construct/flow-outbound-in-middle-of-flow.xml";
    }
    
    @Test
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


