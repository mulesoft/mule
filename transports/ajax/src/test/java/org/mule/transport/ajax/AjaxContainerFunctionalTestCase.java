/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ajax;

import org.mule.transport.ajax.container.MuleAjaxServlet;
import org.mule.transport.servlet.MuleServletContextListener;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

public class AjaxContainerFunctionalTestCase extends AjaxFunctionalTestCase
{

    private Server httpServer;

    public AjaxContainerFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "ajax-container-functional-test-service.xml"},
            {ConfigVariant.FLOW, "ajax-container-functional-test-flow.xml"}
        });
    }      
    
    @Override
    protected void doSetUp() throws Exception
    {
        // FIXME DZ: we don't use the inherited SERVER_PORT here because it's not set
        // at this point and we can't move super.doSetUp() above this
        httpServer = new Server(dynamicPort.getNumber());

        Context c = new Context(httpServer, "/", Context.SESSIONS);
        c.addServlet(new ServletHolder(new MuleAjaxServlet()), "/ajax/*");
        c.addEventListener(new MuleServletContextListener(muleContext, null)); 

        httpServer.start();

        super.doSetUp();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        if (httpServer != null)
        {
            httpServer.stop();
        }
    }

}
