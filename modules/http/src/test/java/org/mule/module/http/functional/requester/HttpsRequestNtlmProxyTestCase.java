/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import static org.mule.module.http.api.HttpHeaders.Names.PROXY_AUTHENTICATE;
import static org.mule.module.http.api.HttpHeaders.Names.PROXY_AUTHORIZATION;

import org.mule.api.MuleEvent;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;

public class HttpsRequestNtlmProxyTestCase extends AbstractNtlmTestCase
{
    @Override
    protected AbstractHandler createHandler(Server server)
    {
        return new ConnectHandler()
        {

            boolean authenticated = false;

            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
            {
                if (request.isSecure())
                {
                    // if the request is secure, the response 
                    // from the https target is being handled
                    simpleResponseFromTarget(response);
                }
                else
                {
                    super.handle(target, baseRequest, request, response);
                }
            }

            @Override
            protected void handleConnect(org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request,
                                         HttpServletResponse response,
                                         String serverAddress)
            {
                super.handleConnect(baseRequest, request, response, serverAddress);
                if (!authenticated)
                {
                    try
                    {
                        response.getOutputStream().flush();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            protected boolean handleAuthentication(HttpServletRequest request, HttpServletResponse response, String address)
            {
                try
                {
                    authenticated = authorizeRequest(address, request, response);
                    return authenticated;
                }
                catch (IOException e)
                {
                    return false;
                }
            }
        };
    }

    public HttpsRequestNtlmProxyTestCase()
    {
        super(PROXY_AUTHORIZATION, PROXY_AUTHENTICATE, SC_PROXY_AUTHENTICATION_REQUIRED);
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
        assertThat(event.getMessage().getPayloadAsString(), equalTo("Authorize"));
    }

    private void simpleResponseFromTarget(HttpServletResponse response) throws IOException
    {
        response.setHeader("Connection", "close");
        response.getOutputStream().print("Authorize");
        response.setStatus(SC_OK);
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }

    protected boolean enableHttps()
    {
        return true;
    }


}
