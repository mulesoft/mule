/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.servlet.MuleReceiverServlet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.ServletHandler;

public class WsdlCallTestCase extends FunctionalTestCase
{
    public static final int HTTP_PORT = 63088;

    private Server httpServer;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        httpServer = new Server();
        SelectChannelConnector conn = new SelectChannelConnector();
        conn.setPort(HTTP_PORT);
        httpServer.addConnector(conn);

        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(MuleReceiverServlet.class, "/services/*");
        
        httpServer.addHandler(handler);
        
        httpServer.start();
    }

    // @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        if (httpServer != null && httpServer.isStarted())
        {
            httpServer.stop();
        }
    }

    public void xtestRequestWsdlWithServlets() throws Exception
    {
        Map<String, String> props = new HashMap<String, String>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:" + HTTP_PORT + "/services/mycomponent?wsdl", null,
            props);

        assertNotNull(result);
        if (logger.isDebugEnabled())
        {
            logger.debug(result.getPayloadAsString());
        }

        String location = "http://localhost:" + HTTP_PORT + "/services/mycomponent?wsdl";
        location = location.substring(0, location.length() - 5);

        assertTrue(result.getStringProperty(HttpConstants.HEADER_CONTENT_TYPE, "").startsWith("text/xml"));

        if (logger.isDebugEnabled())
        {
            logger.debug(result.getPayloadAsString());
        }

        Document document = DocumentHelper.parseText(result.getPayloadAsString());
        List<?> nodes = document.selectNodes("//wsdl:definitions/wsdl:service");
        assertEquals(((Element) nodes.get(0)).attribute("name").getStringValue(), "mycomponent");
    }

    public void testRequestWsdlWithHttp() throws Exception
    {
        MuleClient client = new MuleClient();
        Map<String, String> props = new HashMap<String, String>();
        props.put("http.method", "GET");
        MuleMessage reply = client.send("http://localhost:63082/cxfService?wsdl", null, props);

        assertNotNull(reply);
        assertNotNull(reply.getPayload());

        Document document = DocumentHelper.parseText(reply.getPayloadAsString());
        List<?> nodes = document.selectNodes("//wsdl:definitions/wsdl:service");
        assertEquals(((Element) nodes.get(0)).attribute("name").getStringValue(), "TestServiceComponent");
    }

    protected String getConfigResources()
    {
        return "wsdl-conf.xml";
    }
}
