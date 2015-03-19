/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.construct.FlowConstruct;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpsConnector;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Rule;
import org.junit.runners.Parameterized.Parameters;

public class HttpsFunctionalTestCase extends HttpFunctionalTestCase
{

    public static final String SERVER_KEYSTORE_PATH = "serverKeystorePath";
    public static final String SERVER_KEYSTORE = "serverKeystore";

    @Rule
    public SystemProperty serverKeystoreProperty = new SystemProperty(SERVER_KEYSTORE_PATH, SERVER_KEYSTORE);

    public HttpsFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "https-functional-test-service.xml"},
            {ConfigVariant.FLOW, "https-functional-test-flow.xml"}
        });
    }

    @Override
    public void testSend() throws Exception
    {
        FlowConstruct testSedaService = muleContext.getRegistry().lookupFlowConstruct("testComponent");
        FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent(testSedaService);

        assertNotNull(testComponent);

        final AtomicBoolean callbackMade = new AtomicBoolean(false);
        EventCallback callback = new EventCallback()
        {
            public void eventReceived(final MuleEventContext context, final Object component) throws Exception
            {
                MuleMessage msg = context.getMessage();
                assertTrue(callbackMade.compareAndSet(false, true));
                assertNotNull(msg.getOutboundProperty(HttpsConnector.LOCAL_CERTIFICATES));
            }
        };
        testComponent.setEventCallback(callback);

        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConstants.HEADER_CONTENT_TYPE, "text/plain;charset=UTF-8");
        MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, props);

        assertNotNull(result);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
        assertTrue("Callback never fired", callbackMade.get());
    }
}
