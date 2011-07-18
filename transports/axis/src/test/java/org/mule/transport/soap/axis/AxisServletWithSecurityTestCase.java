/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.transport.PropertyScope;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.servlet.MuleReceiverServlet;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.junit.Rule;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AxisServletWithSecurityTestCase extends FunctionalTestCase
{

    public static int HTTP_PORT = -1;

    private Server httpServer;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "axis-servlet-security-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        HTTP_PORT = dynamicPort.getNumber();
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

    @Test
    public void testSecurityWithServletsUsingGet() throws Exception
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://ross:ross@localhost:" + HTTP_PORT
                                        + "/services/mycomponent?method=echo", "test", props);
        
        String status = result.getProperty(HttpConnector.HTTP_STATUS_PROPERTY, PropertyScope.INBOUND);
        assertEquals(401, new Integer(status).intValue());
        
        MessagingExceptionHandler exceptionListener = 
            muleContext.getRegistry().lookupService("mycomponent").getExceptionListener();
        assertTrue(exceptionListener instanceof UnitTestExceptionStrategy);
        
        UnitTestExceptionStrategy utExStrat = (UnitTestExceptionStrategy)exceptionListener;
        assertEquals(1, utExStrat.getMessagingExceptions().size());
        
        assertNotNull(result);
        // assertTrue(result.getPayload() instanceof byte[]);
    }

}
