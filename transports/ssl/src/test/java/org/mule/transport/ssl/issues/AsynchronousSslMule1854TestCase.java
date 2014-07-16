/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class AsynchronousSslMule1854TestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");

    @Override
    protected String getConfigFile()
    {
        return "ssl-functional-test.xml";
    }

    @Test
    public void testAsynchronous() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("asyncEndpoint", TEST_MESSAGE, null);
        // MULE-2757
        Thread.sleep(500);
        MuleMessage response = client.request("asyncEndpoint", 5000);
        assertNotNull("Response is null", response);
        assertEquals(TEST_MESSAGE + " Received Async", response.getPayloadAsString());
    }
}
