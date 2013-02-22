/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jersey;

import static org.junit.Assert.assertEquals;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.servlet.MuleReceiverServlet;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.junit.Rule;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

public abstract class AbstractServletTestCase extends AbstractServiceAndFlowTestCase
{

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

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

        httpServer = new Server(httpPort.getNumber());

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
