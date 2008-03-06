/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ssl;

import org.mule.api.MuleMessage;
import org.mule.api.service.Service;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.testmodels.mule.TestSedaService;

import java.util.HashMap;
import java.util.Map;

public class SslFunctionalTestCase extends FunctionalTestCase 
{

    protected static String TEST_MESSAGE = "Test Request";
    private static int NUM_MESSAGES = 100;

    protected String getConfigResources()
    {
        return "ssl-functional-test.xml";
    }

    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        MuleMessage result = client.send("sendEndpoint", TEST_MESSAGE, props);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    public void testSendMany() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        for (int i = 0; i < NUM_MESSAGES; ++i)
        {
            MuleMessage result = client.send("sendManyEndpoint", TEST_MESSAGE, props);
            assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
        }

        Service c = muleContext.getRegistry().lookupService("testComponent2");
        assertTrue("Service should be a TestSedaService", c instanceof TestSedaService);
        Object ftc = ((TestSedaService) c).getOrCreateService();
        assertNotNull("Functional Test Service not found in the model.", ftc);
        assertTrue("Service should be a FunctionalTestComponent", ftc instanceof FunctionalTestComponent);

        EventCallback cc = ((FunctionalTestComponent) ftc).getEventCallback();
        assertNotNull("EventCallback is null", cc);
        assertTrue("EventCallback should be a CounterCallback", cc instanceof CounterCallback);
        assertEquals(NUM_MESSAGES, ((CounterCallback) cc).getCallbackCount());
    }

    public void testAsynchronous() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("asyncEndpoint", TEST_MESSAGE, null);
        // MULE-2754
        Thread.sleep(100);
        MuleMessage response = client.request("asyncEndpoint", 5000);
        assertNotNull("Response is null", response);
        assertEquals(TEST_MESSAGE + " Received Async", response.getPayloadAsString());
    }

}
