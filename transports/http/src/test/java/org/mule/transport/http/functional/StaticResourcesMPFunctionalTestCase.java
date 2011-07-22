/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import org.mule.tck.DynamicPortTestCase;
import org.mule.transport.http.HttpConstants;
import org.mule.util.ClassUtils;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

public class StaticResourcesMPFunctionalTestCase extends DynamicPortTestCase
{
    public StaticResourcesMPFunctionalTestCase()
    {
        System.setProperty("test.root", ClassUtils.getClassPathRoot(StaticResourcesMPFunctionalTestCase.class).getPath());
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
    }

    protected String getConfigResources()
    {
        return "http-static-resource-test.xml";
    }

    public void testHttpStaticResource() throws Exception
    {
        String url = String.format("http://localhost:%1d/static", getPorts().get(0));

        GetMethod method = new GetMethod(url);
        HttpClient client = new HttpClient();

        //Test default resource
        int responseCode = client.executeMethod(method);
        assertEquals(HttpConstants.SC_OK, responseCode);

        String result = method.getResponseBodyAsString();
        assertEquals(result, "Test index.html");

        //Test explicit resource
        method = new GetMethod(url + "/main.html");
        responseCode = client.executeMethod(method);
        assertEquals(HttpConstants.SC_OK, responseCode);

        result = method.getResponseBodyAsString();
        assertEquals(result, "Test main.html");

        //Test not found
        method = new GetMethod(url + "/foo.html");
        responseCode = client.executeMethod(method);
        assertEquals(HttpConstants.SC_NOT_FOUND, responseCode);

    }


    public void testHttpStaticResourceMimeTypes() throws Exception
    {
        String url = String.format("http://localhost:%1d/static", getPorts().get(0));

        GetMethod method = new GetMethod(url);
        HttpClient client = new HttpClient();

        //Test default resource
        int responseCode = client.executeMethod(method);
        assertEquals(HttpConstants.SC_OK, responseCode);

        String result = method.getResponseBodyAsString();
        assertEquals(result, "Test index.html");
        assertEquals("text/html", method.getResponseHeader("Content-Type").getValue());

        //Test built in content type
        method = new GetMethod(url + "/image.gif");
        responseCode = client.executeMethod(method);
        assertEquals(HttpConstants.SC_OK, responseCode);
        assertEquals("image/gif", method.getResponseHeader("Content-Type").getValue());

         //Test configured content type (in META-INF/mime.types)
        method = new GetMethod(url + "/image.png");
        responseCode = client.executeMethod(method);
        assertEquals(HttpConstants.SC_OK, responseCode);
        assertEquals("image/png", method.getResponseHeader("Content-Type").getValue());
    }

    public void testHttpsStaticResource() throws Exception
    {
        String url = String.format("https://localhost:%2d/static", getPorts().get(1));

        GetMethod method = new GetMethod(url);
        HttpClient client = new HttpClient();

        //Test default resource
        int responseCode = client.executeMethod(method);
        assertEquals(HttpConstants.SC_OK, responseCode);

        String result = method.getResponseBodyAsString();
        assertEquals(result, "Test index.html");

        //Test explicit resource
        method = new GetMethod(url + "/main.html");
        responseCode = client.executeMethod(method);
        assertEquals(HttpConstants.SC_OK, responseCode);

        result = method.getResponseBodyAsString();
        assertEquals(result, "Test main.html");

        //Test not found
        method = new GetMethod(url + "/foo.html");
        responseCode = client.executeMethod(method);
        assertEquals(HttpConstants.SC_NOT_FOUND, responseCode);

    }

    /**
     * Test that endpoints bound to the same http port but different path work with the
     * static resource MP
     *
     * @throws Exception
     */
    public void testFlowBindingOnSamePort() throws Exception
    {
        String url = String.format("http://localhost:%1d/echo", getPorts().get(0));

        GetMethod method = new GetMethod(url);
        HttpClient client = new HttpClient();

        //Test default resource
        int responseCode = client.executeMethod(method);
        assertEquals(HttpConstants.SC_OK, responseCode);

        assertEquals(method.getResponseBodyAsString(), "/echo");

         url = String.format("https://localhost:%2d/echo", getPorts().get(1));
        method = new GetMethod(url);
        responseCode = client.executeMethod(method);
        assertEquals(HttpConstants.SC_OK, responseCode);

        assertEquals(method.getResponseBodyAsString(), "/echo");
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 2;
    }
}