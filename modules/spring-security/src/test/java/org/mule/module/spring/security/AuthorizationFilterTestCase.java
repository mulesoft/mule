/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.security;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.http.HttpConstants;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AuthorizationFilterTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "http-filter-test.xml";
    }

    @Test
    public void testAuthenticatedButNotAuthorized() throws Exception
    {
        doRequest(null, "localhost", "anon", "anon", getUrl(), false, 405);
    }
    
    @Test
    public void testAuthorized() throws Exception
    {
        doRequest(null, "localhost", "ross", "ross", getUrl(), false, 200);
    }

    @Test
    public void testAuthorizedInAnotherFlow() throws Exception
    {
        doRequest(null, "localhost", "ross", "ross", getUrl(), false, 200);
    }

    protected String getUrl()
    {
        return "http://localhost:4567/authorize";
    }

    private void doRequest(String realm,
                           String host,
                           String user,
                           String pass,
                           String url,
                           boolean handshake,
                           int result) throws Exception
    {
        HttpClient client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(new AuthScope(host, -1, realm),
            new UsernamePasswordCredentials(user, pass));
        GetMethod get = new GetMethod(url);
        get.setDoAuthentication(handshake);

        try
        {
            int status = client.executeMethod(get);
            if (status == HttpConstants.SC_UNAUTHORIZED && handshake == true)
            {
                // doAuthentication = true means that if the request returns 401, 
                // the HttpClient will resend the request with credentials
                status = client.executeMethod(get);
                if (status == HttpConstants.SC_UNAUTHORIZED && handshake == true)
                {
                    // doAuthentication = true means that if the request returns 401, 
                    // the HttpClient will resend the request with credentials
                    status = client.executeMethod(get);
                }
            }
            assertEquals(result, status);
        }
        finally
        {
            get.releaseConnection();
        }
    }

}
