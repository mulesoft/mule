/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;
import org.mule.transport.servlet.JarResourceServlet;

import org.junit.Rule;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.ServletHandler;

import static org.junit.Assert.assertFalse;

public class ResourceLoaderServletTestCase extends AbstractMuleContextTestCase
{
    private Server httpServer;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        httpServer = new Server();
        SelectChannelConnector conn = new SelectChannelConnector();
        conn.setPort(dynamicPort.getNumber());
        httpServer.addConnector(conn);

        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(JarResourceServlet.class, "/mule-resource/*");

        httpServer.addHandler(handler);

        httpServer.start();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        // this generates an exception in GenericServlet which we can safely ignore
        if (httpServer != null)
        {
            httpServer.stop();
            httpServer.destroy();
        }
    }

    @Test
    public void testRetriveJSFromClasspath() throws Exception
    {
        muleContext.start();
        MuleClient client = new MuleClient(muleContext);

        MuleMessage m = client.request("http://localhost:" + dynamicPort.getNumber() + "/mule-resource/js/mule.js", 3000);
        assertFalse(m.getPayload() instanceof NullPayload);
    }
}
