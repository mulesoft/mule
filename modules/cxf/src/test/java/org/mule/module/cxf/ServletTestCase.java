/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertTrue;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.servlet.MuleReceiverServlet;
import org.mule.transport.servlet.jetty.util.EmbeddedJettyServer;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class ServletTestCase extends FunctionalTestCase
{
    public int HTTP_PORT = -1;

    private EmbeddedJettyServer httpServer;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "servlet-conf-flow.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        HTTP_PORT = dynamicPort.getNumber();
        httpServer = new EmbeddedJettyServer(HTTP_PORT, getContextPath(), "/services/*",
            new MuleReceiverServlet(), muleContext);
        httpServer.start();
    }

    protected String getContextPath()
    {
        return "";
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if (httpServer != null && httpServer.isStarted())
        {
            httpServer.stop();
        }

        super.doTearDown();
    }

    @Test
    public void testRequestWsdlWithServlets() throws Exception
    {
        String request = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                         + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                         + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + "<soap:Body>"
                         + "<ns1:echo xmlns:ns1=\"http://testmodels.cxf.module.mule.org/\">"
                         + "<text>Test String</text>" + "</ns1:echo>" + "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("http://localhost:" + HTTP_PORT + getContextPath()
                                         + "/services/mycomponent", getTestMuleMessage(request));
        String res = result.getPayloadAsString();

        assertTrue(res.indexOf("Test String") != -1);
    }

    @Test
    public void testHttpGet() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        MuleMessage result = client.send("http://localhost:" + HTTP_PORT + getContextPath()
                                         + "/services/mycomponent/echo/text/Test String", new DefaultMuleMessage("", props, muleContext));
        String res = result.getPayloadAsString();
        assertTrue(res.indexOf("Test String") != -1);
    }
}
