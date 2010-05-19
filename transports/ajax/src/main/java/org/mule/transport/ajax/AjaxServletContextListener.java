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

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.transport.ajax.container.MuleAjaxServlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class AjaxServletContextListener implements ServletContextListener
{
    private MuleContext muleContext;
    private String connectorName;

    public AjaxServletContextListener(MuleContext context, String name)
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
        event.getServletContext().setAttribute(MuleAjaxServlet.AJAX_CONNECTOR_NAME_PROPERTY, connectorName);
    }
}
