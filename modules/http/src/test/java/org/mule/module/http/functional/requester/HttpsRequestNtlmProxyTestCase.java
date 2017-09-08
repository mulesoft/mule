/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import static org.mule.module.http.api.HttpHeaders.Names.PROXY_AUTHENTICATE;
import static org.mule.module.http.api.HttpHeaders.Names.PROXY_AUTHORIZATION;

import org.mule.api.MuleEvent;
import org.mule.module.http.api.requester.proxy.NtlmConnectHandler;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;

/**
 * This is a simple Functional TestCase that simulates the case of a NTLM HTTP Proxy
 * and an HTTPS Server behind that proxy. Authentication is required.
 */
public class HttpsRequestNtlmProxyTestCase extends AbstractNtlmTestCase
{
    @Override
    protected AbstractHandler createHandler(Server server)
    {
        try
        {
            setupTestAuthorizer(PROXY_AUTHORIZATION, PROXY_AUTHENTICATE, SC_PROXY_AUTHENTICATION_REQUIRED);
            return new NtlmConnectHandler(getAuthorizer());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error creating testAuthorizer");
        }
    }

    @Override
    protected String getConfigFile()
    {
        return "https-request-ntlm-proxy-config.xml";
    }

    @Test
    public void validNtlmAuth() throws Exception
    {
        MuleEvent event = runFlow(getFlowName());
        assertThat((int) event.getMessage().getInboundProperty(HTTP_STATUS_PROPERTY), is(SC_OK));
    }

    protected boolean enableHttps()
    {
        return true;
    }
}
