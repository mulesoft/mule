/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.TraceMethod;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HttpMethodTestCase extends FunctionalTestCase
{

    private HttpMethodBase method;
    private MuleClient muleClient = null;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "http-method-test.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        muleClient = new MuleClient(muleContext);
    }

    @Test
    public void testHead() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new HeadMethod(((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject("inHttpIn")).getAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_OK), Integer.toString(statusCode));

    }

    @Test
    public void testOptions() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new OptionsMethod(((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject("inHttpIn")).getAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_OK), Integer.toString(statusCode));
    }

    @Test
    public void testPut() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new PutMethod(((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject("inHttpIn")).getAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_OK), Integer.toString(statusCode));
    }

    @Test
    public void testDelete() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new DeleteMethod(((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject("inHttpIn")).getAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_OK), Integer.toString(statusCode));
    }

    @Test
    public void testTrace() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new TraceMethod(((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject("inHttpIn")).getAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_OK), Integer.toString(statusCode));
    }

    @Test
    public void testConnect() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new HttpMethodBase(((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject("inHttpIn")).getAddress())
        {
            @Override
            public String getName()
            {
                return "CONNECT";
            }
        };
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_OK), Integer.toString(statusCode));
    }

    @Test
    public void testFoo() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new HttpMethodBase(((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject("inHttpIn")).getAddress())
        {
            @Override
            public String getName()
            {
                return "FOO";
            }
        };
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_BAD_REQUEST), Integer.toString(statusCode));
    }

}


