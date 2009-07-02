/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax.container;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.transport.ajax.i18n.AjaxMessages;
import org.mule.transport.service.TransportFactory;

import javax.servlet.ServletException;

import org.mortbay.cometd.continuation.ContinuationCometdServlet;

/**
 * Wraps the {@link ContinuationCometdServlet} servlet and binds the Bayeux object to
 * the Mule {@link AjaxServletConnector}.
 */
public class MuleAjaxServlet extends ContinuationCometdServlet
{
    /** 
     * The name of the ajax connector to use with this Servlet 
     */
    public static final String AJAX_CONNECTOR_NAME_PROPERTY = "org.mule.ajax.connector.name";

    protected AjaxServletConnector connector = null;

    @Override
    public void init() throws ServletException
    {
        super.init();
        MuleContext muleContext = (MuleContext)getServletContext().getAttribute(MuleProperties.MULE_CONTEXT_PROPERTY);
        if(muleContext==null)
        {
            throw new ServletException("Property " + MuleProperties.MULE_CONTEXT_PROPERTY + " not set on ServletContext");
        }
        String servletConnectorName = getServletConfig().getInitParameter(AJAX_CONNECTOR_NAME_PROPERTY);
        if (servletConnectorName == null)
        {
            connector = (AjaxServletConnector) new TransportFactory(muleContext).getConnectorByProtocol(getConnectorProtocol());
            if (connector == null)
            {
                connector = new AjaxServletConnector();
                try
                {
                    muleContext.getRegistry().registerConnector(connector);
                }
                catch (MuleException e)
                {
                    throw new ServletException("Failed to register the AjaxServletConnector", e);
                }
            }
        }
        else
        {
            connector = (AjaxServletConnector) muleContext.getRegistry().lookupConnector(servletConnectorName);
            if (connector == null)
            {
                throw new ServletException(AjaxMessages.noAjaxConnectorWithName(servletConnectorName, AJAX_CONNECTOR_NAME_PROPERTY).toString());
            }
        }
        connector.setBayeux(getBayeux());
    }

    protected String getConnectorProtocol()
    {
        return AjaxServletConnector.PROTOCOL;
    }
    
}
