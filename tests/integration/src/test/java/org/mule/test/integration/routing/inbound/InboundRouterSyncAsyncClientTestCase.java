/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.routing.inbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class InboundRouterSyncAsyncClientTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/inbound/inbound-router-sync-async-client-test.xml";
    }
    
    @Test
    public void testSync() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        DefaultMuleMessage message = new DefaultMuleMessage("testSync", muleContext);
        message.setOutboundProperty("messageType", "sync");
        MuleMessage result = client.send("vm://singleSyncAsyncEntry", message);
        assertEquals("testSync OK", result.getPayload());
    }

    @Test
    public void testAsync() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        DefaultMuleMessage messsage = new DefaultMuleMessage("testAsync", muleContext);
        messsage.setOutboundProperty("messageType", "async");
        client.dispatch("vm://singleSyncAsyncEntry", messsage);

        MuleMessage result = client.request("vm://asyncResponse", 5000);
        assertNotNull(result);
        assertEquals("testAsync's Response sent to asyncResponse", result.getPayload());
    }
    
}
