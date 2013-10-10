/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
