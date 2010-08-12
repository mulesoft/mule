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

import org.mule.tck.FunctionalTestCase;
import org.mule.transport.servlet.MuleReceiverServlet;
import org.mule.transport.servlet.jetty.util.EmbeddedJettyServer;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class WsdlCallTestCase extends FunctionalTestCase
{
    public static final int HTTP_PORT = 63088;

    private EmbeddedJettyServer httpServer;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        
        httpServer = new EmbeddedJettyServer(HTTP_PORT, "/", "/services/*", new MuleReceiverServlet(), muleContext);
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
        InputStream wsdlStream = new URL("http://localhost:" + HTTP_PORT
            + "/services/mycomponent?wsdl").openStream();
        
        String location = "http://localhost:" + HTTP_PORT + "/services/mycomponent";
        
        Document document = new SAXReader().read(wsdlStream);
        
        List nodes = document.selectNodes("//wsdl:definitions/wsdl:service");
        assertEquals("Callable", ((Element) nodes.get(0)).attribute("name").getStringValue());
        nodes = document.selectNodes("//wsdl:definitions/wsdl:service/wsdl:port/soap:address");
        assertEquals(location, ((Element) nodes.get(0)).attribute("location").getStringValue());
    }

    public void testRequestWsdlWithHttp() throws Exception
    {
        String location = "http://localhost:63082/cxfService";
        InputStream wsdlStream = new URL(location + "?wsdl").openStream();
        
        Document document = new SAXReader().read(wsdlStream);
        List nodes = document.selectNodes("//wsdl:definitions/wsdl:service");
        assertEquals(((Element) nodes.get(0)).attribute("name").getStringValue(), "Callable");
        
        nodes = document.selectNodes("//wsdl:definitions/wsdl:service/wsdl:port/soap:address");
        assertEquals(location, ((Element) nodes.get(0)).attribute("location").getStringValue());
    }

    protected String getConfigResources()
    {
        return "wsdl-conf.xml";
    }
    
}
