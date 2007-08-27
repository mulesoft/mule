/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ssl;

import org.mule.extras.client.MuleClient;
import org.mule.impl.model.MuleProxy;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.testmodels.mule.TestMuleProxy;
import org.mule.tck.testmodels.mule.TestSedaComponent;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.model.UMOModel;

import java.util.HashMap;
import java.util.Map;

public class SslFunctionalTestCase extends FunctionalTestCase {

    protected static String TEST_MESSAGE = "Test Request";
    private static int NUM_MESSAGES = 100;

    public SslFunctionalTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "ssl-functional-test.xml";
    }

    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        UMOMessage result = client.send("sendEndpoint", TEST_MESSAGE, props);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    public void testSendMany() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        for (int i = 0; i < NUM_MESSAGES; ++i)
        {
            UMOMessage result = client.send("sendManyEndpoint", TEST_MESSAGE, props);
            assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
        }

        UMOModel model = managementContext.getRegistry().lookupModel("main");
        UMOComponent c = model.getComponent("testComponent2");
        assertTrue("Component should be a TestSedaComponent", c instanceof TestSedaComponent);
        MuleProxy proxy = ((TestSedaComponent) c).getProxy();
        Object ftc = ((TestMuleProxy) proxy).getComponent();
        assertNotNull("Functional Test Component not found in the model.", ftc);
        assertTrue("Service should be a FunctionalTestComponent", ftc instanceof FunctionalTestComponent);

        EventCallback cc = ((FunctionalTestComponent) ftc).getEventCallback();
        assertNotNull("EventCallback is null", cc);
        assertTrue("EventCallback should be a CounterCallback", cc instanceof CounterCallback);
        assertEquals(NUM_MESSAGES, ((CounterCallback) cc).getCallbackCount());
    }

    // see AsynchronousSslMule1854TestCase
//    public void testAsynchronous() throws Exception
//    {
//        MuleClient client = new MuleClient();
//        client.dispatch("asyncEndpoint", TEST_MESSAGE, null);
//        UMOMessage response = client.receive("asyncEndpoint", 5000);
//        assertNotNull("Response is null", response);
//        assertEquals(TEST_MESSAGE + " Received Async", response.getPayloadAsString());
//    }

}
