/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.servlet.MuleReceiverServlet;

import java.beans.ExceptionListener;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

public class AxisServletWithSecurityTestCase extends FunctionalTestCase
{
    public static final int HTTP_PORT = 18088;

    private Server httpServer;

    protected String getConfigResources()
    {
        return "axis-servlet-security-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        httpServer = new Server(HTTP_PORT);

        Context c = new Context(httpServer, "/", Context.SESSIONS);
        c.addServlet(new ServletHolder(new MuleReceiverServlet()), "/services/*");
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
        if (httpServer != null)
        {
            httpServer.stop();
            httpServer.destroy();
        }
    }

    public void testSecurityWithServletsUsingGet() throws Exception
    {
        Map props = new HashMap();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("http://ross:ross@localhost:" + HTTP_PORT
                                        + "/services/mycomponent?method=echo", "test", props);
        
        ExceptionListener exceptionListener = 
            muleContext.getRegistry().lookupConnector("servletConnector").getExceptionListener();
        assertTrue(exceptionListener instanceof UnitTestExceptionStrategy);
        
        UnitTestExceptionStrategy utExStrat = (UnitTestExceptionStrategy)exceptionListener;
        assertEquals(1, utExStrat.getMessagingExceptions().size());
        
        assertNotNull(result);
        // assertTrue(result.getPayload() instanceof byte[]);
    }
}
