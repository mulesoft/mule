/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.testmodels.mule.TestSedaService;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpsConnector;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import java.util.HashMap;
import java.util.Map;

public class HttpsFunctionalTestCase extends HttpFunctionalTestCase
{
    protected String getConfigResources()
    {
        return "https-functional-test.xml";
    }

    public void testSend() throws Exception
    {
        final TestSedaService testSedaService = (TestSedaService) muleContext.getRegistry().lookupService("testComponent");
        FunctionalTestComponent testComponent = (FunctionalTestComponent) testSedaService.getOrCreateService();
        assertNotNull(testComponent);

        final AtomicBoolean callbackMade = new AtomicBoolean(false);
        EventCallback callback = new EventCallback()
        {
            public void eventReceived(final MuleEventContext context, final Object component) throws Exception
            {
                MuleMessage msg = context.getMessage();
                assertTrue(callbackMade.compareAndSet(false, true));
                assertNotNull(msg.getProperty(HttpsConnector.LOCAL_CERTIFICATES));
            }
        };

        testComponent.setEventCallback(callback);

        MuleClient client = new MuleClient();
        Map props = new HashMap();
        props.put(HttpConstants.HEADER_CONTENT_TYPE, "text/plain;charset=UTF-8");
        MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, props);
        assertNotNull(result);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
        assertTrue("Callback never fired", callbackMade.get());

    }

}
