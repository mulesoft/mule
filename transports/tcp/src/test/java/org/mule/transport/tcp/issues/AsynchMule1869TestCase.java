/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp.issues;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;

import java.util.HashMap;
import java.util.Map;

public class AsynchMule1869TestCase extends DynamicPortTestCase
{

    protected static String TEST_MESSAGE = "Test TCP Request";

    protected String getConfigResources()
    {
        return "tcp-functional-test.xml";
    }

    public void testDispatchAndReply() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map props = new HashMap();
        client.dispatch("asyncClientEndpoint", TEST_MESSAGE, props);
        // MULE-2754
        Thread.sleep(100);
        MuleMessage result =  client.request("asyncClientEndpoint", 10000);
        assertNotNull("No message received", result);
        assertEquals(TEST_MESSAGE + " Received Async", result.getPayloadAsString());
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 2;
    }

}
