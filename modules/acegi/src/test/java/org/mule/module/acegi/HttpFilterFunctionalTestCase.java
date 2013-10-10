/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.acegi;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.http.HttpConstants;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HttpFilterFunctionalTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "http-filter-test.xml";
    }

    @Test
    public void testAuthenticationFailureNoContext() throws Exception
    {
        HttpClient client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(true);
        GetMethod get = new GetMethod("http://localhost:4567/index.html");

        get.setDoAuthentication(false);

        try
        {
            int status = client.executeMethod(get);
            assertEquals(HttpConstants.SC_UNAUTHORIZED, status);
            assertEquals(
                    "Registered authentication is set to org.mule.module.acegi.filters.http.HttpBasicAuthenticationFilter "
                    + "but there was no security context on the session. Authentication denied on endpoint "
                    + "http://localhost:4567. Message payload is of type: String",
                    get.getResponseBodyAsString());
        }
        finally
        {
            get.releaseConnection();
        }
    }

    @Test
    public void testAuthenticationFailureBadCredentials() throws Exception
    {
        doRequest(null, "localhost", "anonX", "anonX", "http://localhost:4567/index.html", true, false, 401);
    }

    @Test
    public void testAuthenticationAuthorised() throws Exception
    {
        doRequest(null, "localhost", "anon", "anon", "http://localhost:4567/index.html", false, true, 200);
    }

    @Test
    public void testAuthenticationAuthorisedWithHandshake() throws Exception
    {
        doRequest(null, "localhost", "anon", "anon", "http://localhost:4567/index.html", true, false, 200);
    }

    @Test
    public void testAuthenticationAuthorisedWithHandshakeAndBadRealm() throws Exception
    {
        doRequest("blah", "localhost", "anon", "anon", "http://localhost:4567/index.html", true, false, 401);
    }

    @Test
    public void testAuthenticationAuthorisedWithHandshakeAndRealm() throws Exception
    {
        doRequest("mule-realm", "localhost", "ross", "ross", "http://localhost:4567/index.html", true, false,
                  200);
    }

    private void doRequest(String realm,
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

}
