/*
 * $Id: XFireWsdlCallTestCase.java 5558 2007-03-13 17:52:34Z Lajos $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.mortbay.http.HttpContext;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.util.InetAddrPort;
import org.mule.extras.client.MuleClient;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.providers.http.servlet.MuleReceiverServlet;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class WsdlCallTestCase extends FunctionalTestCase
{
    public static final int HTTP_PORT = 63088;

    private Server httpServer;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        httpServer = new Server();
        SocketListener socketListener = new SocketListener(new InetAddrPort(HTTP_PORT));
//        httpServer.addListener(socketListener);

//        HttpContext context = httpServer.getContext("/");
//        context.setRequestLog(null);

        ServletHandler handler = new ServletHandler();
//        handler.addServlet("MuleReceiverServlet", "/services/*", MuleReceiverServlet.class.getName());

//        context.addHandler(handler);
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

    public void testRequestWsdlWithServlets() throws Exception
    {
        Map<String, String> props = new HashMap<String, String>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("http://localhost:" + HTTP_PORT + "/services/mycomponent?wsdl", null,
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
        List nodes = document.selectNodes("//wsdl:definitions/wsdl:service");
        assertEquals(((Element) nodes.get(0)).attribute("name").getStringValue(), "mycomponent");
    }

    public void testRequestWsdlWithHttp() throws Exception
    {
        MuleClient client = new MuleClient();
        Map<String, String> props = new HashMap<String, String>();
        props.put("http.method", "GET");
        UMOMessage reply = client.send("http://localhost:63082/cxfService?wsdl", null, props);

        assertNotNull(reply);
        assertNotNull(reply.getPayload());

        Document document = DocumentHelper.parseText(reply.getPayloadAsString());
        List nodes = document.selectNodes("//wsdl:definitions/wsdl:service");
        assertEquals(((Element) nodes.get(0)).attribute("name").getStringValue(), "cxfService");
    }

    protected String getConfigResources()
    {
        return "wsdl-conf.xml";
    }
}
