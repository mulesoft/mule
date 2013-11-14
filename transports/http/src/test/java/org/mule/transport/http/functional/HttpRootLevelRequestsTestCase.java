/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;

/**
 * See MULE-6968 "Http endpoint with path="" or path="/" do not attend requests at root level"
 */
public class HttpRootLevelRequestsTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort httpPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort httpPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort httpPort3 = new DynamicPort("port3");

    @Rule
    public DynamicPort httpPort4 = new DynamicPort("port4");

    @Rule
    public DynamicPort httpPort5 = new DynamicPort("port5");


    @Override
    protected String getConfigFile()
    {
        return "http-root-level-requests.xml";
    }

    @Test
    public void acceptsRootRequestsInHttpEndpointWithoutPath() throws IOException
    {
        assertRootResponseOk(httpPort1.getValue());
    }

    @Test
    public void acceptsRootRequestsInHttpEndpointWithEmptyPath() throws IOException
    {
        assertRootResponseOk(httpPort2.getValue());
    }

    @Test
    public void acceptsRootRequestsInHttpEndpointWithRootPath() throws IOException
    {
        assertRootResponseOk(httpPort3.getValue());
    }

    @Test
    public void acceptsRootRequestsInHttpAddressWithoutTrailingSlash() throws IOException
    {
        assertRootResponseOk(httpPort4.getValue());
    }

    @Test
    public void acceptsRootRequestsInHttpAddressWithTrailingSlash() throws IOException
    {
        assertRootResponseOk(httpPort5.getValue());
    }

    private void assertRootResponseOk(String port) throws IOException
    {
        String url = "http://localhost:" + port;
        assertResponseOk(url);
        assertResponseOk(url + "/");
    }

    private void assertResponseOk(String url) throws IOException
    {
        HttpClient client = new HttpClient();
        GetMethod getMethod = new GetMethod(url);
        int statusCode = client.executeMethod(getMethod);
        assertEquals(HttpStatus.SC_OK, statusCode);
    }
}
