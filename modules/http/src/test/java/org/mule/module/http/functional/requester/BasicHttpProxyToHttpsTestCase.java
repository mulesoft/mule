/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.functional.requester;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import org.mule.api.MuleEvent;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;

public class BasicHttpProxyToHttpsTestCase extends AbstractHttpRequestTestCase
{

    private static final String AUTHORIZED = "Authorized";
    private static final String PROXY_PASSWORD = "dXNlcjpwYXNzd29yZA==";
    private static final String PASSWORD = "am9obmRvZTpwYXNz";

    private Server httpsServer;

    @Override
    protected AbstractHandler createHandler(Server server)
    {
        return new ProxyConnectHTTPHandler();
    }

    @Override
    protected boolean enableHttps()
    {
        return true;
    }

    @Override
    public void startServer() throws Exception
    {
        super.startServer();
        httpsServer.start();
    }

    @Override
    public void stopServer() throws Exception
    {
        httpsServer.stop();
        super.stopServer();
    }

    @Override
    protected void enableHttpsServer(Server server)
    {
        httpsServer = new Server();
        httpsServer.setHandler(new AuthenticateHandler(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException
            {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().print(AUTHORIZED);

                baseRequest.setHandled(true);
            }
        }));
        super.enableHttpsServer(httpsServer);
    }

    private static class ProxyConnectHTTPHandler extends ConnectHandler {

        @Override
        protected boolean handleAuthentication(HttpServletRequest request, HttpServletResponse response, String address) {
            return true;
        }

        /**
         * Override this method do to the {@link ConnectHandler#handleConnect(org.eclipse.jetty.server.Request, HttpServletRequest, HttpServletResponse, String)} doesn't allow me to generate a response with
         * {@link HttpServletResponse#SC_PROXY_AUTHENTICATION_REQUIRED} neither {@link HttpServletResponse#SC_UNAUTHORIZED}.
         */
        @Override
        protected void handleConnect(org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response, String serverAddress)
        {
            try
            {
                if (!this.doHandleAuthentication(baseRequest, response))
                {
                    return;
                }
            }
            catch (Exception e)
            {
                return;
            }

            // Just call super class method to establish the tunnel and avoid copy/paste.
            super.handleConnect(baseRequest, request, response, serverAddress);
        }

        public boolean doHandleAuthentication(org.eclipse.jetty.server.Request request, HttpServletResponse httpResponse) throws IOException, ServletException {
            boolean result = false;
            if ("CONNECT" == (request.getMethod()))
            {
                String authorization = request.getHeader("Proxy-Authorization");
                if (authorization == null)
                {
                    httpResponse.setStatus(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
                    httpResponse.setHeader("Proxy-Authenticate", "Basic realm=\"Fake Realm\"");
                    result = false;
                }
                else if (authorization.equals("Basic " + PROXY_PASSWORD))
                {
                    httpResponse.setStatus(HttpServletResponse.SC_OK, "Connection established");
                    result = true;
                }
                else
                {
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    httpResponse.getOutputStream().flush();
                    httpResponse.getOutputStream().close();
                    result = false;
                }
                httpResponse.getOutputStream().flush();
                httpResponse.getOutputStream().close();
                request.setHandled(true);
            }
            return result;
        }
    }

    private static class AuthenticateHandler extends AbstractHandler {

        private Handler target;

        public AuthenticateHandler(Handler target) {
            this.target = target;
        }

        @Override
        public void handle(String pathInContext, org.eclipse.jetty.server.Request request, HttpServletRequest httpRequest,
                           HttpServletResponse httpResponse) throws IOException, ServletException {
            String authorization = httpRequest.getHeader("Authorization");
            if (authorization != null && authorization.equals("Basic " + PASSWORD))
            {
                httpResponse.addHeader("target", request.getUri().toString());
                target.handle(pathInContext, request, httpRequest, httpResponse);
            }
            else
            {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setHeader("www-authenticate", "Basic realm=\"Fake Realm\"");
                httpResponse.getOutputStream().flush();
                httpResponse.getOutputStream().close();
                request.setHandled(true);
            }

        }
    }

    @Override
    protected String getConfigFile()
    {
        return "http-request-proxy-to-https-config.xml";
    }

    /**
     * Validates that during the CONNECT the HTTP proxy should pass the proxy authentication
     * when accessing to an HTTPS.
     * https://github.com/AsyncHttpClient/async-http-client/issues/1152
     *
     * @throws Exception
     */
    @Test
    public void validProxyHttpConnectToHttpsAuth() throws Exception
    {
        MuleEvent event = runFlow("httpFlow");

        assertThat((int) event.getMessage().getInboundProperty(HTTP_STATUS_PROPERTY), is(SC_OK));
        assertThat(event.getMessage().getPayloadAsString(), equalTo(AUTHORIZED));
    }

}
