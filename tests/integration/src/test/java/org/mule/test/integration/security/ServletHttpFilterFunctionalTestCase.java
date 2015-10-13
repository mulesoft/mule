/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.security;

import org.mule.module.spring.security.HttpFilterFunctionalTestCase;
import org.mule.transport.servlet.MuleReceiverServlet;
import org.mule.transport.servlet.jetty.util.EmbeddedJettyServer;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class ServletHttpFilterFunctionalTestCase extends HttpFilterFunctionalTestCase
{

    public static final int HTTP_PORT = 4567;

    private EmbeddedJettyServer httpServer;

    public ServletHttpFilterFunctionalTestCase(String configResources)
    {
        super(configResources);

    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
                {"org/mule/test/integration/security/servlet-http-filter-test-service.xml"},
                {"org/mule/test/integration/security/servlet-http-filter-test-flow.xml"}
        });
    }

    @Override
    protected String getUrl()
    {
        return "http://localhost:" + HTTP_PORT + "/test/index.html";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        httpServer = new EmbeddedJettyServer(HTTP_PORT, "/", "/*", new MuleReceiverServlet(), muleContext);
        httpServer.start();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if (httpServer != null && httpServer.isStarted())
        {
            httpServer.stop();
        }

        super.doTearDown();
    }

    protected String getNoContextErrorResponse()
    {
        return "Registered authentication is set to org.mule.module.spring.security.filters.http.HttpBasicAuthenticationFilter "
               + "but there was no security context on the session. Authentication denied on endpoint "
               + "servlet://test.";
    }

}
