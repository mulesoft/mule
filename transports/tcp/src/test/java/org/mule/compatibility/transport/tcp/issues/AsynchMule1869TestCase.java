/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class AsynchMule1869TestCase extends FunctionalTestCase
{
    protected static String TEST_MESSAGE = "Test TCP Request";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Override
    protected String getConfigFile()
    {
        return "tcp-functional-test-flow.xml";
    }

    @Test
    public void testDispatchAndReply() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        client.dispatch("asyncClientEndpoint", TEST_MESSAGE, props);
        // MULE-2754
        Thread.sleep(100);

        MuleMessage result =  client.request("asyncClientEndpoint", 10000);
        assertNotNull("No message received", result);
        assertEquals(TEST_MESSAGE + " Received Async", getPayloadAsString(result));
    }

}
