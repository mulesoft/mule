/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.NullPayload;
import org.mule.module.client.MuleClient;
import org.mule.api.MuleMessage;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;

public class ResourceLoaderServletTestCase extends AbstractMuleTestCase
{
     private Server httpServer;

    // @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        httpServer = new Server();
        SelectChannelConnector conn = new SelectChannelConnector();
        conn.setPort(8881);
        httpServer.addConnector(conn);

        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(MuleJarResourcesServlet.class, "/mule-resource/*");

        httpServer.addHandler(handler);

        httpServer.start();
    }

    // @Override
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

    public void testRetriveJSFromClasspath() throws Exception
    {
        MuleClient client = new MuleClient();

        MuleMessage m = client.request("http://localhost:8881/mule-resource/web/js/mule.js", 3000);
        assertFalse(m.getPayload() instanceof NullPayload);
    }
}
