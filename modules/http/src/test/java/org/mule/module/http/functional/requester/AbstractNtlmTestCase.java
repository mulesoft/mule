/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;

import org.mule.api.MuleEvent;
import org.mule.module.http.api.requester.proxy.NtlmProxyTestAuthorizer;
import org.mule.module.http.api.requester.proxy.TestAuthorizer;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Test;

public abstract class AbstractNtlmTestCase extends AbstractHttpRequestTestCase
{
    protected static final String USER = "Zaphod";
    protected static final String PASSWORD = "Beeblebrox";
    protected static final String DOMAIN = "Ursa-Minor";
    private static final String AUTHORIZED = "Authorized";

    protected String requestUrl;

    private TestAuthorizer authorizer;

    public void setupTestAuthorizer(String clientAuthHeader, String serverAuthHeader, int unauthorizedHeader)
    {
        authorizer = createTestAuthorizer(clientAuthHeader, serverAuthHeader, unauthorizedHeader);
    }

    protected String getDomain()
    {
        return DOMAIN;
    }

    protected String getWorkstation()
    {
        return null;
    }

    protected void handleRequest(String address, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if(authorizer.authorizeRequest(address, request, response, true))
        {
            requestUrl = address;
        }
    }

    private TestAuthorizer createTestAuthorizer(String clientAuthHeader, String serverAuthHeader, int unauthorizedHeader)
    {
        try
        {
            TestAuthorizer testAuthorizer = new NtlmProxyTestAuthorizer.Builder().setClientAuthHeader(clientAuthHeader)
                                                                                 .setServerAuthHeader(serverAuthHeader)
                                                                                 .setUnauthorizedHeader(unauthorizedHeader)
                                                                                 .setUser(USER)
                                                                                 .setPassword(PASSWORD)
                                                                                 .setDomain(getDomain())
                                                                                 .setWorkstation(getWorkstation())
                                                                                 .build();
            return testAuthorizer;

        }
        catch (Exception e)
        {
            throw new RuntimeException("Error creating testAuthorizer");
        }
    }
    
    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        handleRequest(baseRequest.getRequestURL().toString(), request, response);
    }

    @Test
    public void validNtlmAuth() throws Exception
    {
        MuleEvent event = runFlow(getFlowName());

        assertThat((int) event.getMessage().getInboundProperty(HTTP_STATUS_PROPERTY), is(SC_OK));
        assertThat(event.getMessage().getPayloadAsString(), equalTo(AUTHORIZED));
    }

    protected String getFlowName()
    {
        return "ntlmFlow";
    }

    public TestAuthorizer getAuthorizer()
    {
        return authorizer;
    }
}
