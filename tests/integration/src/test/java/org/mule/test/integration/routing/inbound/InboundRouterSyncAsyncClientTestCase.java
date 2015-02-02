/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

@Ignore("MULE-4485")
public class InboundRouterSyncAsyncClientTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/inbound/inbound-router-sync-async-client-test.xml";
    }

    @Test
    public void testSync() throws Exception
    {
        MuleClient client = muleContext.getClient();
        DefaultMuleMessage message = new DefaultMuleMessage("testSync", muleContext);
        message.setOutboundProperty("messageType", "sync");
        MuleMessage result = client.send("vm://singleSyncAsyncEntry", message);
        assertEquals("testSync OK", result.getPayload());
    }

    @Test
    public void testAsync() throws Exception
    {
        MuleClient client = muleContext.getClient();
        DefaultMuleMessage messsage = new DefaultMuleMessage("testAsync", muleContext);
        messsage.setOutboundProperty("messageType", "async");
        client.dispatch("vm://singleSyncAsyncEntry", messsage);

        MuleMessage result = client.request("vm://asyncResponse", 5000);
        assertNotNull(result);
        assertEquals("testAsync's Response sent to asyncResponse", result.getPayload());
    }
}
