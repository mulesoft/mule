/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.service.Service;
import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.testmodels.mule.TestSedaService;
import org.mule.util.JdkVersionUtils;
import org.mule.util.JdkVersionUtils.JdkVersion;

import org.junit.Rule;
import org.junit.Test;

public class SslFunctionalTestCase extends FunctionalTestCase
{
    private static int NUM_MESSAGES = 100;

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

    @Override
    protected boolean isExcluded()
    {
        // exclude these tests if we're running in Java 7 because ssl is broken in
        // the jdk
        return super.isExcluded() || System.getProperty("java.version").matches("1\\.7\\..*")
        		|| JdkVersionUtils.getJdkVersion().compareTo(new JdkVersion("1.6.0_26")) > 0;
    }

    @Test
    public void testSend() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("sendEndpoint", TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    @Test
    public void testSendMany() throws Exception
    {
        MuleClient client = muleContext.getClient();
        for (int i = 0; i < NUM_MESSAGES; ++i)
        {
            MuleMessage result = client.send("sendManyEndpoint", TEST_MESSAGE, null);
            assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
        }

        Service c = muleContext.getRegistry().lookupService("testComponent2");
        assertTrue("Service should be a TestSedaService", c instanceof TestSedaService);
        Object ftc = getComponent(c);
        assertNotNull("Functional Test Service not found in the model.", ftc);
        assertTrue("Service should be a FunctionalTestComponent", ftc instanceof FunctionalTestComponent);

        EventCallback cc = ((FunctionalTestComponent) ftc).getEventCallback();
        assertNotNull("EventCallback is null", cc);
        assertTrue("EventCallback should be a CounterCallback", cc instanceof CounterCallback);
        assertEquals(NUM_MESSAGES, ((CounterCallback) cc).getCallbackCount());
    }

    @Test
    public void testAsynchronous() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("asyncEndpoint", TEST_MESSAGE, null);
        // MULE-2757
        Thread.sleep(100);
        MuleMessage response = client.request("asyncEndpoint", 5000);
        assertNotNull("Response is null", response);
        assertEquals(TEST_MESSAGE + " Received Async", response.getPayloadAsString());
    }
}
