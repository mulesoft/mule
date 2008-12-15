/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.inbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;


public class InboundRouterSyncAsyncClientTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/inbound/inbound-router-sync-async-client-test.xml";
    }
    
    public void testSync() throws Exception
    {
        MuleClient client = new MuleClient();
        DefaultMuleMessage message = new DefaultMuleMessage("testSync");
        message.setProperty("messageType", "sync");
        MuleMessage result = client.send("vm://singleSyncAsyncEntry", message);
        assertEquals("testSync OK", result.getPayload());
    }

    public void testAsync() throws Exception
    {
        MuleClient client = new MuleClient();
        DefaultMuleMessage messsage = new DefaultMuleMessage("testAsync");
        messsage.setProperty("messageType", "async");
        client.dispatch("vm://singleSyncAsyncEntry", messsage);

        MuleMessage result = client.request("vm://asyncResponse", 5000);
        assertEquals("testAsync's Response sent to asyncResponse", result.getPayload());
    }
}
