/*
 * $Id: HttpFilterFunctionalTestCase.java 20320 2010-11-24 15:03:31Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.spring.security;

import org.mule.tck.FunctionalTestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;

public class AuthorizationFilterTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "http-filter-test.xml";
    }

    public void testAuthenticatedButNotAuthorized() throws Exception
    {
        doRequest(null, "localhost", "anon", "anon", getUrl(), true, false, 405);
    }
    
    public void testAuthorized() throws Exception
    {
        doRequest(null, "localhost", "ross", "ross", getUrl(), true, false, 200);
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
