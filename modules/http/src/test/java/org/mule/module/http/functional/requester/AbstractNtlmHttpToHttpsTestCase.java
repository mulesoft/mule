/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.mule.module.http.functional.proxy.ConfigurationUtil.configureHttpsServer;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

public abstract class AbstractNtlmHttpToHttpsTestCase extends FunctionalTestCase
{
    private static final String PROXY_AUTHORIZATION = "Proxy-Authorization";

    private static final String NTML_HEADER_VALUE = "NTLM";

    private static final String PROXY_AUTHENTICATE_HEADER = "Proxy-Authenticate";

    private static final String TYPE_1_MESSAGE = "NTLM TlRMTVNTUAABAAAAAYIIogAAAAAoAAAAAAAAACgAAAAFASgKAAAADw==";

    private static final String TYPE_2_MESSAGE = "NTLM TlRMTVNTUAACAAAAAAAAACgAAAABggAAU3J2Tm9uY2UAAAAAAAAAAA==";

    private static final String TYPE_2_CHALLENGE_MESSAGE =
            "NTLM TlRMTVNTUAADAAAAGAAYAEgAAAAYABgAYAAAABQAFAB4AAAADAAMAIwAAAAQABAAmAAAAAAAAACoAAAAAYIAAgUBKAoAAAAPrYfKbe/jRoW5xDxHeoxC1gBmfWiS5+iX4OAN4xBKG/IFPwfH3agtPEia6YnhsADTVQBSAFMAQQAtAE0ASQBOAE8AUgBaAGEAcABoAG8AZABFAFAASQBMAEEAUAAzADMA";

    protected static final String TARGET_SERVER_RESPONSE = "Response";

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");
    @Rule
    public DynamicPort httpsPort = new DynamicPort("httpsPort");

    protected Server proxyServer;

    protected Server httpsSever;

    @Before
    public void startServers() throws Exception
    {
        configureProxy();
        configureHttpsTargetServer();
    }


    private void configureHttpsTargetServer() throws Exception
    {
        httpsSever = createServer(httpsPort.getNumber(), true);
        httpsSever.setHandler(new TestHandler());
        httpsSever.start();
    }


    private void configureProxy() throws Exception
    {
        proxyServer = createServer(httpPort.getNumber(), false);
        proxyServer.setHandler(configureHandler());
        proxyServer.start();
    }

    public static class TestHandler extends AbstractHandler
    {

        @Override
        public void handle(String pathInContext,
                           Request request,
                           HttpServletRequest httpRequest,
                           HttpServletResponse httpResponse)
                throws IOException, ServletException
        {
            httpResponse.setHeader("Connection", "close");
            httpResponse.getOutputStream().print(TARGET_SERVER_RESPONSE);
            httpResponse.setStatus(SC_OK);
            httpResponse.getOutputStream().flush();
            httpResponse.getOutputStream().close();
        }
    }


    @After
    public void stopServer() throws Exception
    {
        proxyServer.stop();
        httpsSever.stop();
    }


    public AbstractHandler configureHandler() throws Exception
    {
        return new ConnectHandler(new TestHandler())
        {
            boolean authenticated = false;

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
                String authorization = request.getHeader(PROXY_AUTHORIZATION);
                if (authorization == null)
                {
                    response.setStatus(SC_PROXY_AUTHENTICATION_REQUIRED);
                    response.setHeader(PROXY_AUTHENTICATE_HEADER, NTML_HEADER_VALUE);
                    return false;
                }
                else if (authorization.equals(TYPE_1_MESSAGE))
                {
                    response.setStatus(SC_PROXY_AUTHENTICATION_REQUIRED);
                    response.setHeader(PROXY_AUTHENTICATE_HEADER, TYPE_2_MESSAGE);
                    return false;
                }
                else if (authorization.equals(TYPE_2_CHALLENGE_MESSAGE))
                {
                    response.setStatus(SC_OK);
                    authenticated = true;
                    return true;
                }
                else
                {
                    response.setStatus(SC_UNAUTHORIZED);
                    return false;
                }
            }
        };
    }

    protected Server createServer(int port, boolean secure)
    {
        Server server = null;
        if (secure)
        {
            server = new Server();
            configureHttpsServer(server, httpsPort.getNumber(), getClass());
        }
        else
        {
            server = new Server(port);
        }

        return server;
    }
}
