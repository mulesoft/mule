/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.junit.Assert.fail;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static sun.net.www.protocol.http.AuthScheme.NTLM;

import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.module.http.internal.request.ResponseValidatorException;
import org.mule.util.FileUtils;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.security.Constraint;

public class HttpRequestMultipleAuthenticationMethodsSupportedTestCase extends AbstractHttpRequestTestCase
{
    private static int basicRequestCount = 0;
    private static int digestRequestCount = 0;

    @Override
    protected String getConfigFile() { return "http-request-multiple-auth-methods-config.xml"; }

    @Test
    public void onMultipleAuthMethodChoiceLocalAuthSchemeIsSet() throws Exception
    {
        try
        {
            Flow flow = (Flow) getFlowConstruct("digestAuthRequest");
            MuleEvent event = getTestEvent(TEST_MESSAGE);

            event.setFlowVariable("user", "user");
            event.setFlowVariable("password", "password");
            event.setFlowVariable("preemptive", true);

            event = flow.process(event);

            assertThat(event.getMessage().getPayloadAsString(), equalTo(DEFAULT_RESPONSE));
            assertThat(digestRequestCount, is(2));
            assertThat(basicRequestCount, is(0));
        }
        catch(ResponseValidatorException e)
        {
            fail("Request UNAUTHORIZED!");
        }

    }

    @Override
    protected AbstractHandler createHandler(Server server)
    {
        AbstractHandler handler = super.createHandler(server);
        String realmPath = null;

        try
        {
            realmPath = FileUtils.getResourcePath("auth/realm.properties", getClass());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        LoginService loginService = new HashLoginService("TestRealm", realmPath);
        server.addBean(loginService);

        final Constraint digestConstraint = new Constraint();
        digestConstraint.setName("auth");
        digestConstraint.setRoles(new String[] {"user"});
        digestConstraint.setAuthenticate(true);

        ConstraintMapping digestConstraintMapping = new ConstraintMapping();
        digestConstraintMapping.setConstraint(digestConstraint);
        digestConstraintMapping.setPathSpec("/*");

        ConstraintSecurityHandler digestSecurityHandler = new ConstraintSecurityHandler()
        {
            @Override
            public void handle(String pathInContext, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                digestRequestCount++;
                super.handle(pathInContext, baseRequest, request, response);
            }
        };

        digestSecurityHandler.setAuthenticator(new MyDigestAuthenticator());
        digestSecurityHandler.setConstraintMappings(new ConstraintMapping[] {digestConstraintMapping});

        ContextHandler digestContext = new ContextHandler("/digest");
        digestContext.setHandler(digestSecurityHandler);
        digestSecurityHandler.setHandler(handler);

        ContextHandlerCollection handlers = new ContextHandlerCollection();
        handlers.setHandlers(new Handler[] { digestContext });

        return handlers;
    }

    private class MyDigestAuthenticator extends DigestAuthenticator
    {
        public Authentication validateRequest(ServletRequest req, ServletResponse res, boolean mandatory) throws ServerAuthException
        {
            HttpServletRequest request = (HttpServletRequest)req;
            String wwwAuthHeader = ((HttpServletRequest) req).getHeader(HttpHeader.WWW_AUTHENTICATE.asString());
            String authHeader = ((HttpServletRequest) req).getHeader(HttpHeader.AUTHORIZATION.asString());
            if(wwwAuthHeader == null && authHeader == null)
            {
                try
                {
                    HttpServletResponse response = (HttpServletResponse)res;
                    response.setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), "Basic realm=\"" + this._loginService.getName() + "\"");
                    response.addHeader(HttpHeader.WWW_AUTHENTICATE.asString(), NTLM.toString());
                    response.addHeader(HttpHeader.WWW_AUTHENTICATE.asString(), "Digest realm=\"" + this._loginService.getName() + "\", domain=\"/digest\", nonce=\"" + this.newNonce((Request)request) + "\", algorithm=MD5, qop=\"auth\", stale=false");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return Authentication.SEND_CONTINUE;
                }
                catch (IOException var14)
                {
                    throw new ServerAuthException(var14);
                }
            }
            else
            {
                return super.validateRequest(req, res, mandatory);
            }

        }
    }
}
