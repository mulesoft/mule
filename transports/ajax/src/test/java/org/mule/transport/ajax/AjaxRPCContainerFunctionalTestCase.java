/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ajax;

import org.mule.api.config.MuleProperties;
import org.mule.transport.ajax.container.MuleAjaxServlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

public class AjaxRPCContainerFunctionalTestCase extends AjaxRPCFunctionalTestCase
{
    private Server httpServer;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        httpServer = new Server(58883);

        Context c = new Context(httpServer, "/", Context.SESSIONS);
        c.addServlet(new ServletHolder(new MuleAjaxServlet()), "/cometd/*");
        c.addEventListener(new ServletContextListener() {
            public void contextInitialized(ServletContextEvent sce)
            {
                sce.getServletContext().setAttribute(MuleProperties.MULE_CONTEXT_PROPERTY, muleContext);
            }

            public void contextDestroyed(ServletContextEvent sce) { }
        });

        httpServer.start();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if(httpServer!=null) httpServer.stop();
    }

    @Override
    protected String getConfigResources()
    {
        return "ajax-container-rpc-test.xml";
    }
}