/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.PatchMethod;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.TraceMethod;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;

public class HttpMethodTestCase extends AbstractServiceAndFlowTestCase
{
    @ClassRule
    public static DynamicPort dynamicPort = new DynamicPort("port1");

    private HttpClient client;

    public HttpMethodTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setDisposeContextPerClass(true);
        client = new HttpClient();
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-method-test-service.xml"},
            {ConfigVariant.FLOW, "http-method-test-flow.xml"}
        });
    }

    @Test
    public void testHead() throws Exception
    {
        HeadMethod method = new HeadMethod(getHttpEndpointAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(HttpStatus.SC_OK, statusCode);
    }

    @Test
    public void testOptions() throws Exception
    {
        OptionsMethod method = new OptionsMethod(getHttpEndpointAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(HttpStatus.SC_OK, statusCode);
    }

    @Test
    public void testPut() throws Exception
    {
        PutMethod method = new PutMethod(getHttpEndpointAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(HttpStatus.SC_OK, statusCode);
    }

    @Test
    public void testDelete() throws Exception
    {
        DeleteMethod method = new DeleteMethod(getHttpEndpointAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(HttpStatus.SC_OK, statusCode);
    }

    @Test
    public void testTrace() throws Exception
    {
        TraceMethod method = new TraceMethod(getHttpEndpointAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(HttpStatus.SC_OK, statusCode);
    }

    @Test
    public void testConnect() throws Exception
    {
        CustomHttpMethod method = new CustomHttpMethod(HttpConstants.METHOD_CONNECT, getHttpEndpointAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(HttpStatus.SC_OK, statusCode);
    }

    @Test
    public void testPatch() throws Exception
    {
        PatchMethod method = new PatchMethod(getHttpEndpointAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(HttpStatus.SC_OK, statusCode);
    }

    @Test
    public void testFoo() throws Exception
    {
        CustomHttpMethod method = new CustomHttpMethod("FOO", getHttpEndpointAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(HttpStatus.SC_BAD_REQUEST, statusCode);
    }

    private String getHttpEndpointAddress()
    {
        InboundEndpoint httpEndpoint = muleContext.getRegistry().lookupObject("inHttpIn");
        return httpEndpoint.getAddress();
    }

    private static class CustomHttpMethod extends HttpMethodBase
    {
        private final String method;

        public CustomHttpMethod(String method, String url)
        {
            super(url);
            this.method = method;
        }

        @Override
        public String getName()
        {
            return method;
        }
    }
}
