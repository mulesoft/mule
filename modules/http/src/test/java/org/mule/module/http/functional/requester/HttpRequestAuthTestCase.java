/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.FileUtils;

import java.io.IOException;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.security.Constraint;
import org.junit.Test;

public class HttpRequestAuthTestCase extends AbstractHttpRequestTestCase
{

    public HttpRequestAuthTestCase(boolean nonBlocking)
    {
        super(nonBlocking);
    }

    @Override
    protected String getConfigFile()
    {
        return "http-request-auth-config.xml";
    }

    @Test
    public void validBasicAuthentication() throws Exception
    {
        assertValidRequest("basicAuthRequest");
    }

    @Test
    public void validDigestAuth() throws Exception
    {
        assertValidRequest("digestAuthRequest");
    }

    private void assertValidRequest(String flowName) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);

        MuleEvent event = getTestEvent(TEST_MESSAGE);

        event = flow.process(event);

        assertThat(event.getMessage().getPayloadAsString(), equalTo(DEFAULT_RESPONSE));
    }

    @Override
    protected AbstractHandler createHandler(Server server)
    {
        AbstractHandler handler = super.createHandler(server);

        String realmPath = null;

        try
        {
            realmPath = FileUtils.getResourcePath("realm.properties", getClass());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        LoginService loginService = new HashLoginService("TestRealm", realmPath);
        server.addBean(loginService);

        Constraint basicConstraint = new Constraint();
        basicConstraint.setName("auth");
        basicConstraint.setRoles(new String[] {"user"});
        basicConstraint.setAuthenticate(true);

        ConstraintMapping basicConstraintMapping = new ConstraintMapping();
        basicConstraintMapping.setConstraint(basicConstraint);
        basicConstraintMapping.setPathSpec("/*");

        ConstraintSecurityHandler basicSecurityHandler = new ConstraintSecurityHandler();
        basicSecurityHandler.setAuthenticator(new BasicAuthenticator());
        basicSecurityHandler.setConstraintMappings(new ConstraintMapping[] {basicConstraintMapping});

        ContextHandler basicContext = new ContextHandler("/basic");
        basicContext.setHandler(basicSecurityHandler);


        Constraint digestConstraint = new Constraint();
        digestConstraint.setName("auth");
        digestConstraint.setRoles(new String[] {"user"});
        digestConstraint.setAuthenticate(true);

        ConstraintMapping digestConstraintMapping = new ConstraintMapping();
        digestConstraintMapping.setConstraint(digestConstraint);
        digestConstraintMapping.setPathSpec("/*");

        ConstraintSecurityHandler digestSecurityHandler = new ConstraintSecurityHandler();
        digestSecurityHandler.setAuthenticator(new DigestAuthenticator());
        digestSecurityHandler.setConstraintMappings(new ConstraintMapping[] {digestConstraintMapping});

        ContextHandler digestContext = new ContextHandler("/digest");
        digestContext.setHandler(digestSecurityHandler);

        basicSecurityHandler.setHandler(handler);
        digestSecurityHandler.setHandler(handler);

        ContextHandlerCollection handlers = new ContextHandlerCollection();
        handlers.setHandlers(new Handler[] {basicContext, digestContext});

        return handlers;
    }

}
