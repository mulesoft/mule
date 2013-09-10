/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.servlet.JarResourceServlet;
import org.mule.transport.servlet.jetty.util.EmbeddedJettyServer;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JarResourceServletTestCase extends AbstractMuleContextTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    private EmbeddedJettyServer server;

    @Before
    public void startEmbeddedJettyServer() throws Exception
    {
        server = new EmbeddedJettyServer(port1.getNumber(), "/", "/mule-resource/*",
            new JarResourceServlet(), muleContext);
        server.start();
    }

    @After
    public void shutdownEmbeddedJettyServer() throws Exception
    {
        if (server != null)
        {
            server.stop();
            server.destroy();
        }
    }

    @Test
    public void retriveHtmlFromClasspath() throws Exception
    {
        muleContext.start();

        String result = getContentsOfResource("foo.html");
        assertTrue(result.contains("${title}"));

        String replacement = "hello foo";
        muleContext.getRegistry().registerObject("title", replacement);

        result = getContentsOfResource("foo.html");
        assertTrue(result.contains(replacement));
    }

    @Test
    public void retriveXmlFromClasspath() throws Exception
    {
        muleContext.start();

        String result = getContentsOfResource("foo.xml");
        assertTrue(result.contains("${bar}"));

        String replacement = "hello bar";
        muleContext.getRegistry().registerObject("bar", replacement);

        result = getContentsOfResource("foo.xml");
        assertTrue(result.contains(replacement));
    }

    private String getContentsOfResource(String resource) throws IOException
    {
        String url = String.format("http://localhost:%d/mule-resource/files/%s", port1.getNumber(),
            resource);
        GetMethod method = new GetMethod(url);
        int rc = new HttpClient().executeMethod(method);
        assertEquals(HttpStatus.SC_OK, rc);
        return method.getResponseBodyAsString();
    }
}
