/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.functional;

import org.mule.extras.client.MuleClient;
import org.mule.providers.http.HttpConstants;
import org.mule.providers.http.HttpsConnector;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.testmodels.mule.TestMuleProxy;
import org.mule.tck.testmodels.mule.TestSedaComponent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.model.UMOModel;

import java.util.HashMap;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

public class HttpsFunctionalTestCase extends HttpFunctionalTestCase
{
    protected String getConfigResources()
    {
        return "https-functional-test.xml";
    }

    public void testSend() throws Exception
    {
        UMOModel model = managementContext.getRegistry().lookupModel("main");

        final TestSedaComponent testSedaComponent = (TestSedaComponent) model.getComponent("testComponent");
        TestMuleProxy proxy = (TestMuleProxy) testSedaComponent.getProxy();
        FunctionalTestComponent testComponent = (FunctionalTestComponent) proxy.getComponent();
        assertNotNull(testComponent);

        final AtomicBoolean callbackMade = new AtomicBoolean(false);
        EventCallback callback = new EventCallback()
        {
            public void eventReceived(final UMOEventContext context, final Object component) throws Exception
            {
                UMOMessage msg = context.getMessage();
                assertTrue(callbackMade.compareAndSet(false, true));
                assertNotNull(msg.getProperty(HttpsConnector.LOCAL_CERTIFICATES));
            }
        };

        testComponent.setEventCallback(callback);

        MuleClient client = new MuleClient();
        Map props = new HashMap();
        props.put(HttpConstants.HEADER_CONTENT_TYPE, "text/plain;charset=UTF-8");
        UMOMessage result = client.send("clientEndpoint", TEST_MESSAGE, props);
        assertNotNull(result);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
        assertTrue("Callback never fired", callbackMade.get());

    }

}
