/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
