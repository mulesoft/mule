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
import org.mule.transport.servlet.MuleReceiverServlet;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;

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
        
        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(MuleReceiverServlet.class, "/services/*");
        httpServer.addHandler(handler);
        
        httpServer.start();
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
            "<echo xmlns=\"http://simple.components.mule.org/\">" +
            "<echo>Test String</echo>" +
            "</echo>" +
            "</soap:Body>" +
            "</soap:Envelope>";
        
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://localhost:" + HTTP_PORT
                                        + "/services/mycomponent", request, null);
        String res = result.getPayloadAsString();
        
        assertTrue(res.indexOf("Test String") != -1);
    }

}


