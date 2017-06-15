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
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class BasicHttpProxyToHttpTestCase extends AbstractHttpRequestTestCase
{

    private static final String AUTHORIZED = "Authorized";
    private static final String PROXY_PASSWORD = "dXNlcjpwYXNzd29yZA==";
    private static final String PASSWORD = "am9obmRvZTpwYXNz";

    @Rule
    public DynamicPort targetPort = new DynamicPort("targetPort");


    @Override
    protected AbstractHandler createHandler(Server server)
    {
        return new ProxyHTTPHandler();
    }

    /**
     * Just one handler that works as proxy and target server in order to simplify the test, first it checks for the proxy
     * authorization header and then for the authorization header.
     */
    public static class ProxyHTTPHandler extends AbstractHandler
    {

        @Override
        public void handle(String pathInContext, org.eclipse.jetty.server.Request request, HttpServletRequest httpRequest,
                           HttpServletResponse httpResponse) throws IOException, ServletException {

            String authorization = httpRequest.getHeader("Authorization");
            String proxyAuthorization = httpRequest.getHeader("Proxy-Authorization");
            if (proxyAuthorization == null)
            {
                httpResponse.setStatus(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
                httpResponse.setHeader("Proxy-Authenticate", "Basic realm=\"Fake Realm\"");
            }
            else if (proxyAuthorization
                .equals("Basic " + PROXY_PASSWORD) && authorization != null && authorization.equals("Basic " + PASSWORD))
            {
                httpResponse.getOutputStream().print(AUTHORIZED);
                httpResponse.setStatus(HttpServletResponse.SC_OK);
            }
            else
            {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setHeader("www-authenticate", "Basic realm=\"Fake Realm\"");
            }
            httpResponse.getOutputStream().flush();
            httpResponse.getOutputStream().close();
            request.setHandled(true);
        }
    }

    @Override
    protected String getConfigFile()
    {
        return "http-request-proxy-to-http-config.xml";
    }

    /**
     * Validates that authentication header to HTTP are passed when using a proxy .
     * https://github.com/AsyncHttpClient/async-http-client/issues/1321
     *
     * @throws Exception
     */
    @Test
    @Ignore("MULE-12766 - Test that validates the fix from grizzly ahc https://github.com/javaee/grizzly-ahc/issues/3, "
        + "should be enabled once library is migrated to the new release")
    public void validProxyHttpConnectToHttpAuth() throws Exception
    {
        MuleEvent event = runFlow("httpFlow");

        assertThat((int) event.getMessage().getInboundProperty(HTTP_STATUS_PROPERTY), is(SC_OK));
        assertThat(event.getMessage().getPayloadAsString(), equalTo(AUTHORIZED));
    }

}
