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
import org.mule.tck.DynamicPortTestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.TraceMethod;

public class HttpMethodTestCase extends DynamicPortTestCase
{

    private HttpMethodBase method;
    private MuleClient muleClient = null;
        
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        muleClient = new MuleClient(muleContext);
    }

    @Override
    protected String getConfigResources()
    {
        return "http-method-test.xml";
    }

    protected void doFunctionalTearDown () throws Exception
    {
        if (method != null)
        {
            method.releaseConnection();
        }
    }

    public void testHead() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new HeadMethod(((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject("inHttpIn")).getAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_OK), Integer.toString(statusCode));

    }

    public void testOptions() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new OptionsMethod(((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject("inHttpIn")).getAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_OK), Integer.toString(statusCode));
    }

    public void testPut() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new PutMethod(((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject("inHttpIn")).getAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_OK), Integer.toString(statusCode));
    }

    public void testDelete() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new DeleteMethod(((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject("inHttpIn")).getAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_OK), Integer.toString(statusCode));
    }

    public void testTrace() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new TraceMethod(((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject("inHttpIn")).getAddress());
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_OK), Integer.toString(statusCode));
    }

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

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }
}


