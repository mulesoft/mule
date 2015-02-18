/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import static org.junit.Assert.assertEquals;
import static org.mule.module.http.api.HttpConstants.Methods.DELETE;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.servlet.MuleReceiverServlet;

import javax.servlet.ServletContext;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Rule;

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

        ServletContextHandler root = new ServletContextHandler(httpServer, "/", ServletContextHandler.SESSIONS);
        ServletHolder holder = new ServletHolder(MuleReceiverServlet.class);
        root.addServlet(holder, context);

        ServletContext servletContext = root.getServletContext();
        servletContext.setAttribute(MuleProperties.MULE_CONTEXT_PROPERTY, muleContext);

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
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.send(root + "/helloworld", getTestMuleMessage(), newOptions().method(POST.name()).build());
        assertEquals((Integer)200, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
        assertEquals("Hello World", result.getPayloadAsString());

        result = client.send(root + "/hello", getTestMuleMessage(), newOptions().disableStatusCodeValidation().build());
        assertEquals((Integer)404, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));

        result = client.send(root + "/helloworld", getTestMuleMessage(), newOptions().disableStatusCodeValidation().build());
        assertEquals((Integer)405, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));

        result = client.send(root + "/helloworld", getTestMuleMessage(), newOptions().method(DELETE.name()).disableStatusCodeValidation().build());
        assertEquals("Hello World Delete", result.getPayloadAsString());
        assertEquals((Integer)200, result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0));
    }
}
