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
import org.mule.api.config.MuleProperties;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.servlet.MuleReceiverServlet;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

public class ServletTestCase extends FunctionalTestCase
{

    public static final int HTTP_PORT = 63088;

    private Server httpServer;

    @Override
    protected String getConfigResources()
    {
        return "servlet-conf.xml";
    }

    @Override
    protected void doSetUp() throws Exception 
    {
        super.doSetUp();
        
        httpServer = new Server(HTTP_PORT);
        String path = getContextPath();
        if (path.equals("")) path = "/";
        
        Context c = new Context(httpServer, path, Context.SESSIONS);
        c.addServlet(new ServletHolder(new MuleReceiverServlet()), "/services/*");
        c.addEventListener(new ServletContextListener() {
            public void contextInitialized(ServletContextEvent sce)
            {
                sce.getServletContext().setAttribute(MuleProperties.MULE_CONTEXT_PROPERTY, muleContext);
            }

            public void contextDestroyed(ServletContextEvent sce) { }
        });
        
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

    public void testRequestWsdlWithServlets() throws Exception
    {
        String request = 
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
            "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
            "<soap:Body>" +
            "<ns1:echo xmlns:ns1=\"http://testmodels.cxf.transport.mule.org/\">" +
            "<text>Test String</text>" +
            "</ns1:echo>" +
            "</soap:Body>" +
            "</soap:Envelope>";
        
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:" + HTTP_PORT
                                        + getContextPath() + "/services/mycomponent", request, null);
        String res = result.getPayloadAsString();

        assertTrue(res.indexOf("Test String") != -1);
    }

    public void testHttpGet() throws Exception
    {
        MuleClient client = new MuleClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        MuleMessage result = client.send("http://localhost:" + HTTP_PORT
                                        + getContextPath() + "/services/mycomponent/echo/text/Test String", "", props);
        String res = result.getPayloadAsString();
        assertTrue(res.indexOf("Test String") != -1);
    }
}


