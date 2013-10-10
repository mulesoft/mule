/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    public ServletHttpFilterFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);

    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/security/servlet-http-filter-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/security/servlet-http-filter-test-flow.xml"}});
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
               + "servlet://test. Message payload is of type: String";
    }

}
