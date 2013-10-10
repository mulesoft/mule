/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.jetty.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
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
    protected String getConfigResources()
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
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                MuleMessage msg = context.getMessage();
                assertEquals(HttpConstants.METHOD_POST, msg.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY));
                assertEquals("/normal", msg.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
                assertEquals("/normal", msg.getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY));
                assertEquals("/normal", msg.getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY));
            }
        };

        testComponent.setEventCallback(callback);
        
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/normal", TEST_MESSAGE, null);
        assertEquals("200", response.getInboundProperty("http.status"));
        assertEquals(TEST_MESSAGE + " received", IOUtils.toString((InputStream) response.getPayload()));
    }
    
    @Test
    public void testExceptionExecutionFlow() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/exception", TEST_MESSAGE, null);
        assertEquals("500", response.getInboundProperty("http.status"));
        assertNotNull(response.getExceptionPayload());
    }

}
