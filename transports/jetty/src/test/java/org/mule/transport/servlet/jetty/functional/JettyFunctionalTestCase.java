/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty.functional;

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
import org.mule.transport.http.HttpConstants;
import org.mule.util.IOUtils;

import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;

/**
 * Functional tests specific to Jetty.
 */
public class JettyFunctionalTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "jetty-functional-test.xml";
    }

    @Test
    public void testNormalExecutionFlow() throws Exception
    {
        FunctionalTestComponent testComponent = getFunctionalTestComponent("normalExecutionFlow");
        assertNotNull(testComponent);

        EventCallback callback = new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                MuleMessage msg = context.getMessage();
                assertEquals(HttpConstants.METHOD_POST, msg.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY));
                assertEquals("/normal?param1=value1", msg.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
                assertEquals("/normal", msg.getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY));
                assertEquals("/normal", msg.getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY));
                assertEquals("http://localhost:" + dynamicPort.getValue() + "/normal", msg.getInboundProperty(HttpConnector.HTTP_CONTEXT_URI_PROPERTY));
                assertNotNull(msg.getInboundProperty(HttpConnector.HTTP_QUERY_PARAMS));
                assertNotNull(msg.getInboundProperty(HttpConnector.HTTP_HEADERS));
            }
        };

        testComponent.setEventCallback(callback);

        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/normal?param1=value1", TEST_MESSAGE, null);
        assertEquals("200", response.getInboundProperty("http.status"));
        assertEquals(TEST_MESSAGE + " received", IOUtils.toString((InputStream) response.getPayload()));
    }

    @Test
    public void testExceptionExecutionFlow() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/exception", TEST_MESSAGE, null);
        assertEquals("500", response.getInboundProperty("http.status"));
        assertNotNull(response.getExceptionPayload());
    }
}
