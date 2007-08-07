/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.functional;

import org.mule.tck.FunctionalTestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.TraceMethod;

public class HttpMethodTestCase extends FunctionalTestCase
{

    private HttpMethodBase method;

    public HttpMethodTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

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
        method = new HeadMethod("http://localhost:60200");
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_OK), Integer.toString(statusCode));

    }

    public void testOptions() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new OptionsMethod("http://localhost:60200");
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_METHOD_NOT_ALLOWED), Integer.toString(statusCode));
    }

    public void testPut() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new PutMethod("http://localhost:60200");
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_METHOD_NOT_ALLOWED), Integer.toString(statusCode));
    }

    public void testDelete() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new DeleteMethod("http://localhost:60200");
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_METHOD_NOT_ALLOWED), Integer.toString(statusCode));
    }

    public void testTrace() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new TraceMethod("http://localhost:60200");
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_METHOD_NOT_ALLOWED), Integer.toString(statusCode));
    }

    public void testConnect() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new HttpMethodBase("http://localhost:60200")
        {
            public String getName()
            {
                return "CONNECT";
            }
        };
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_METHOD_NOT_ALLOWED), Integer.toString(statusCode));
    }

    public void testFoo() throws Exception
    {
        HttpClient client = new HttpClient();
        method = new HttpMethodBase("http://localhost:60200")
        {
            public String getName()
            {
                return "FOO";
            }
        };
        int statusCode = client.executeMethod(method);
        assertEquals(Integer.toString(HttpStatus.SC_BAD_REQUEST), Integer.toString(statusCode));
    }
}


