/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AuthorizationFilterTestCase extends FunctionalTestCase
{

    private final String configFile;
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    public AuthorizationFilterTestCase(String configFile)
    {
        this.configFile = configFile;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"http-transport-filter-test.xml"},
                {"http-module-filter-test.xml"}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    @Test
    public void testNotAuthenticated() throws Exception
    {
        doRequest("localhost",getUrl(), 401);
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
        return "http://localhost:" + port1.getNumber() + "/authorize";
    }

    private void doRequest(String host,
                           String url,
                           int result) throws Exception
    {
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(url);
        try
        {
            int status = client.executeMethod(get);
            assertEquals(status,result);
            assertNotNull(get.getResponseHeader("WWW-Authenticate"));
            assertThat(get.getResponseHeader("WWW-Authenticate").getValue().contains("mule-realm"), Is.is(true));
        }
        finally
        {
            get.releaseConnection();
        }
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
