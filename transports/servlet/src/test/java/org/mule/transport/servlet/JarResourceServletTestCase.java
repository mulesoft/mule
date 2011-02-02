/*
 * $Id: ResourceLoaderServletTestCase.java 20321 2010-11-24 15:21:24Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.servlet.jetty.util.EmbeddedJettyServer;

public class JarResourceServletTestCase extends AbstractMuleTestCase
{
    private EmbeddedJettyServer server;

    public JarResourceServletTestCase()
    {
        super();
        // use dynamic ports outside of a FunctionalTestCase 
        numPorts = 1;
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        this.server = new EmbeddedJettyServer(getPorts().get(0), "/", "/mule-resource/*", new JarResourceServlet(), muleContext);
        this.server.start();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        // this generates an exception in GenericServlet which we can safely ignore
        if (this.server != null)
        {
            this.server.stop();
            this.server.destroy();
        }
    }

    public void testRetriveJSFromClasspath() throws Exception
    {
        muleContext.start();

        LocalMuleClient client = muleContext.getClient();

        MuleMessage m = client.request("http://localhost:" + getPorts().get(0) + "/mule-resource/files/foo.html", 3000);
        String result = m.getPayloadAsString();
        assertTrue(result.contains("${title}"));

        muleContext.getRegistry().registerObject("title", "hello foo");

         m = client.request("http://localhost:" + getPorts().get(0) + "/mule-resource/files/foo.html", 3000);
        result = m.getPayloadAsString();
        assertTrue(result.contains("hello foo"));

    }

    public void testRetriveJSFromClasspathXmlFilter() throws Exception
    {
        muleContext.start();

        LocalMuleClient client = muleContext.getClient();

        MuleMessage m = client.request("http://localhost:" + getPorts().get(0) + "/mule-resource/files/foo.xml", 3000);
        String result = m.getPayloadAsString();
        assertTrue(result.contains("${bar}"));

        muleContext.getRegistry().registerObject("bar", "hello bar");

         m = client.request("http://localhost:" + getPorts().get(0) + "/mule-resource/files/foo.xml", 3000);
        result = m.getPayloadAsString();
        assertTrue(result.contains("hello bar"));

    }
}
