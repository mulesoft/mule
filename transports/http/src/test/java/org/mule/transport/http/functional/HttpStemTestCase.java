/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;

import org.junit.Rule;
import org.junit.Test;

public class HttpStemTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "http-stem-test.xml";
    }

    @Test
    public void testStemMatching() throws Exception
    {
        MuleClient client = muleContext.getClient();
        int port = dynamicPort.getNumber();
        doTest(client, "http://localhost:" + port + "/foo", "/foo", "/foo");
        doTest(client, "http://localhost:" + port + "/foo/baz", "/foo", "/foo/baz");
        doTest(client, "http://localhost:" + port + "/bar", "/bar", "/bar");
        doTest(client, "http://localhost:" + port + "/bar/baz", "/bar", "/bar/baz");
    }

    protected void doTest(MuleClient client, final String url, final String contextPath, final String requestPath) throws Exception
    {
        FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent(contextPath);
        assertNotNull(testComponent);

        EventCallback callback = new EventCallback()
        {
            @Override
            public void eventReceived(final MuleEventContext context, final Object component) throws Exception
            {
                MuleMessage msg = context.getMessage();
                assertEquals(requestPath, msg.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
                assertEquals(requestPath, msg.getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY));
                assertEquals(contextPath, msg.getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY));
            }
        };

        testComponent.setEventCallback(callback);

        MuleMessage result = client.send(url, "Hello World", null);
        assertEquals("Hello World Received", result.getPayloadAsString());
        final int status = result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0);
        assertEquals(200, status);
    }
}
