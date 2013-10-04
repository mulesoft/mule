/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty.util;

import org.mortbay.jetty.Connector;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;

import javax.servlet.Servlet;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

/**
 * A simple helper class for Mule testing that creates an embedded Jetty Server
 */
public class EmbeddedJettyServer
{
    private Server httpServer;

    public EmbeddedJettyServer(int port, String contextPath, String servletPath, Servlet servlet, final MuleContext context)
    {
        httpServer = new Server(port);
        for (Connector connector : httpServer.getConnectors())
        {
            connector.setHeaderBufferSize(16384);
        }
        Context c = new Context(httpServer, contextPath, Context.SESSIONS);
        c.addServlet(new ServletHolder(servlet), servletPath);
        c.addEventListener(new ServletContextListener()
        {
            public void contextInitialized(ServletContextEvent sce)
            {
                sce.getServletContext().setAttribute(MuleProperties.MULE_CONTEXT_PROPERTY, context);
            }

            public void contextDestroyed(ServletContextEvent sce)
            {
            }
        });
    }

    public void start() throws Exception
    {
        httpServer.start();
    }

    public void stop() throws Exception
    {
        httpServer.stop();
    }

    public void destroy()
    {
        httpServer.destroy();
    }

    public boolean isStarted()
    {
        return httpServer.isStarted();
    }
}
