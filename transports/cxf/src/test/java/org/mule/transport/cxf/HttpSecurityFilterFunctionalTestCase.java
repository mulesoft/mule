/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConstants;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

public class HttpSecurityFilterFunctionalTestCase extends FunctionalTestCase
{
    
    private static String soapRequest = 
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:unk=\"http://unknown.namespace/\">" +
           "<soapenv:Header/>" +
           "<soapenv:Body>" +
              "<unk:echo>" +         
                 "<arg0>asdf</arg0>" +
              "</unk:echo>" +
           "</soapenv:Body>" +
        "</soapenv:Envelope>";

    protected String getConfigResources()
    {
        return "http-security-filter-test.xml";
    }

    /**
     * By putting this test method that uses https first we can test MULE-4558
     * 
     * @throws Exception
     */
    public void testAuthenticationFailureBadCredentialsGetHttps() throws Exception
    {
        doGet(null, "localhost", "anonX", "anonX", "https://localhost:60444/services/Echo", true, true, 401);
    }

    public void testAuthenticationFailureNoContextGet() throws Exception
    {
        HttpClient client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(true);
        GetMethod get = new GetMethod("http://localhost:60443/services/Echo");

        get.setDoAuthentication(false);

        try
        {
            int status = client.executeMethod(get);
            assertEquals(HttpConstants.SC_UNAUTHORIZED, status);
            assertEquals("/services/Echo", get.getResponseBodyAsString());
        }
        finally
        {
            get.releaseConnection();
        }
    }

    public void testAuthenticationFailureNoContextPost() throws Exception
    {
        HttpClient client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(true);
        PostMethod post = new PostMethod("http://localhost:60443/services/Echo");

        post.setDoAuthentication(false);

        StringRequestEntity requestEntity = new StringRequestEntity(soapRequest, "text/xml", "UTF-8");
        post.setRequestEntity(requestEntity);

        try
        {
            int status = client.executeMethod(post);
            assertEquals(HttpConstants.SC_UNAUTHORIZED, status);
            // TODO MULE-4561 Why do we return request and not an error?
            assertEquals(soapRequest, post.getResponseBodyAsString());
        }
        finally
        {
            post.releaseConnection();
        }
    }

    public void testAuthenticationFailureBadCredentialsGet() throws Exception
    {
        doGet(null, "localhost", "anonX", "anonX", "http://localhost:60443/services/Echo/echo/echo/hello", true, true, 401);
    }

    public void testAuthenticationFailureBadCredentialsPost() throws Exception
    {
        doPost(null, "localhost", "anonX", "anonX", "http://localhost:60443/services/Echo", true, true, 401);
    }

    public void testAuthenticationFailureBadCredentialsPostHttps() throws Exception
    {
        doPost(null, "localhost", "anonX", "anonX", "https://localhost:60444/services/Echo", true, true, 401);
    }

    public void testAuthenticationAuthorisedGet() throws Exception
    {
        doGet(null, "localhost", "anon", "anon", "http://localhost:60443/services/Echo/echo/echo/hello", false, true, 200);
    }

    public void testAuthenticationAuthorisedGetHttps() throws Exception
    {
        doGet(null, "localhost", "anon", "anon", "https://localhost:60444/services/Echo/echo/echo/hello", false, true, 200);
    }

    public void testAuthenticationAuthorisedPost() throws Exception
    {
        doPost(null, "localhost", "anon", "anon", "http://localhost:60443/services/Echo", false, true, 200);
    }

    public void testAuthenticationAuthorisedPostHttps() throws Exception
    {
        doPost(null, "localhost", "anon", "anon", "https://localhost:60444/services/Echo", false, true, 200);
    }

    public void testAuthenticationAuthorisedWithHandshakeGet() throws Exception
    {
        doGet(null, "localhost", "anon", "anon", "http://localhost:60443/services/Echo/echo/echo/hello", true, false, 200);
    }

    public void testAuthenticationAuthorisedWithHandshakePost() throws Exception
    {
        doPost(null, "localhost", "anon", "anon", "http://localhost:60443/services/Echo", true, false, 200);
    }

    public void testAuthenticationAuthorisedWithHandshakeAndBadRealmGet() throws Exception
    {
        doGet("blah", "localhost", "anon", "anon", "http://localhost:60443/services/Echo/echo/echo/hello", true, false, 401);
    }

    public void testAuthenticationAuthorisedWithHandshakeAndBadRealmPost() throws Exception
    {
        doPost("blah", "localhost", "anon", "anon", "http://localhost:60443/services/Echo", true, false, 401);
    }

    public void testAuthenticationAuthorisedWithHandshakeAndRealmGet() throws Exception
    {
        doGet("mule-realm", "localhost", "ross", "ross", "http://localhost:60443/services/Echo/echo/echo/hello", true, false,
            200);
    }

    public void testAuthenticationAuthorisedWithHandshakeAndRealmPost() throws Exception
    {
        doPost("mule-realm", "localhost", "ross", "ross", "http://localhost:60443/services/Echo", true,
            false, 200);
    }

    private void doGet(String realm,
                       String host,
                       String user,
                       String pass,
                       String url,
                       boolean handshake,
                       boolean preemtive,
                       int result) throws Exception
    {
        HttpClient client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(preemtive);
        client.getState().setCredentials(new AuthScope(host, -1, realm),
            new UsernamePasswordCredentials(user, pass));
        GetMethod get = new GetMethod(url);
        get.setDoAuthentication(handshake);

        try
        {
            int status = client.executeMethod(get);
            assertEquals(result, status);
        }
        finally
        {
            get.releaseConnection();
        }
    }

    private void doPost(String realm,
                        String host,
                        String user,
                        String pass,
                        String url,
                        boolean handshake,
                        boolean preemtive,
                        int result) throws Exception
    {
        HttpClient client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(preemtive);
        client.getState().setCredentials(new AuthScope(host, -1, realm),
            new UsernamePasswordCredentials(user, pass));
        PostMethod post = new PostMethod(url);
        post.setDoAuthentication(handshake);
        StringRequestEntity requestEntity = new StringRequestEntity(soapRequest, "text/xml", "UTF-8");
        post.setRequestEntity(requestEntity);
        try
        {
            int status = client.executeMethod(post);
            assertEquals(result, status);
            // TODO MULE-4561 Why do we return request and not an error?
            assertNotNull(post.getResponseBodyAsString());
        }
        finally
        {
            post.releaseConnection();
        }
    }

}
