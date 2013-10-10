/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jersey;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.servlet.MuleReceiverServlet;

public abstract class AbstractServletTestCase extends AbstractServiceAndFlowTestCase
{

    //TODO(pablo.kraan): replace with a dynamic endpoint
    public static final int HTTP_PORT = 63088;

    private Server httpServer;
    private String context;
    
    public AbstractServletTestCase(ConfigVariant variant, String configResources, String context)
    {
        super(variant, configResources);
        this.context = context;
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        httpServer = new Server(HTTP_PORT);

        Context root = new Context(httpServer,"/",Context.SESSIONS);
        ServletHolder holder = new ServletHolder(MuleReceiverServlet.class);
        root.addServlet(holder, context);

        ServletContext context = root.getServletContext();
        context.setAttribute(MuleProperties.MULE_CONTEXT_PROPERTY, muleContext);
        
        httpServer.start();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        if (httpServer != null && httpServer.isStarted())
        {
            httpServer.stop();
        }
    }

    public void doTestBasic(String root) throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage result = client.send(root + "/helloworld", "", null);
        assertEquals((Integer)200, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertEquals("Hello World", result.getPayloadAsString());

        result = client.send(root + "/hello", "", null);
        assertEquals((Integer)404, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));

        Map<String, String> props = new HashMap<String, String>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);
        result = client.send(root + "/helloworld", "", props);
        assertEquals((Integer)405, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));

        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_DELETE);
        result = client.send(root + "/helloworld", "", props);
        assertEquals("Hello World Delete", result.getPayloadAsString());
        assertEquals((Integer)200, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
    }

}
