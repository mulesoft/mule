/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.testmodels.mule.TestSedaService;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.functional.HttpFunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JettyHttpsFunctionalTestCase extends HttpFunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "jetty-https-functional-test.xml";
    }

    @Override
    public void testSend() throws Exception
    {
        final TestSedaService testSedaService = (TestSedaService) muleContext.getRegistry().lookupService("testComponent");
        FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent(testSedaService);
        assertNotNull(testComponent);

        final AtomicBoolean callbackMade = new AtomicBoolean(false);
        EventCallback callback = new EventCallback()
        {
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                assertTrue(callbackMade.compareAndSet(false, true));
                MuleMessage msg = context.getMessage();
                assertEquals(TEST_MESSAGE, msg.getPayloadAsString());
            }
        };

        testComponent.setEventCallback(callback);

        MuleClient client = new MuleClient(muleContext);
        Map<String, String> props = new HashMap<String, String>();
        props.put(HttpConstants.HEADER_CONTENT_TYPE, "text/plain;charset=UTF-8");
        MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, props);
        assertNotNull(result);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
        assertTrue("Callback never fired", callbackMade.get());
    }
}
