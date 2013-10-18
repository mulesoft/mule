/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
