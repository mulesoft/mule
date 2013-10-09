/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class MuleServletContextListener implements ServletContextListener
{
    public static final String CONNECTOR_NAME = "mule.connector.name";

    private MuleContext muleContext;
    private String connectorName;

    public MuleServletContextListener(MuleContext context, String name)
    {
        super();
        muleContext = context;
        connectorName = name;
    }
    
    public void contextDestroyed(ServletContextEvent sce)
    {
        // nothing to do
    }

    public void contextInitialized(ServletContextEvent event)
    {
        event.getServletContext().setAttribute(MuleProperties.MULE_CONTEXT_PROPERTY, muleContext);
        //We keep this for backward compatability
        event.getServletContext().setAttribute(AbstractReceiverServlet.SERVLET_CONNECTOR_NAME_PROPERTY, connectorName);
        event.getServletContext().setAttribute(CONNECTOR_NAME, connectorName);
    }
}
