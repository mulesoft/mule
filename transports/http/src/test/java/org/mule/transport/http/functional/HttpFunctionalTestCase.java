/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HttpFunctionalTestCase extends FunctionalTestCase
{
    protected static String TEST_MESSAGE = "Test Http Request (R�dgr�d), 57 = \u06f7\u06f5 in Arabic";
    protected boolean checkPathProperties = true;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "http-functional-test.xml";
    }

    @Test
    public void testSend() throws Exception
    {        
        FunctionalTestComponent testComponent = getFunctionalTestComponent("testComponent");
        assertNotNull(testComponent);

        if (checkPathProperties) 
        {
            EventCallback callback = new EventCallback()
            {
                public void eventReceived(final MuleEventContext context, final Object component) throws Exception
                {
                    MuleMessage msg = context.getMessage();
                    assertEquals("/", msg.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
                    assertEquals("/", msg.getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY));
                    assertEquals("/", msg.getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY));
                }
            };
        
            testComponent.setEventCallback(callback);
        }

        MuleClient client = new MuleClient(muleContext);
        Map<String, String> props = new HashMap<String, String>();
        props.put(HttpConstants.HEADER_CONTENT_TYPE, "text/plain;charset=UTF-8");
        MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, props);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

}
