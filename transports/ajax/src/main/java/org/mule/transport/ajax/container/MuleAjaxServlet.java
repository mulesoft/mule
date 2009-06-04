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

import org.mule.RegistryContext;
import org.mule.api.MuleException;
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
        String servletConnectorName = getServletConfig().getInitParameter(AJAX_CONNECTOR_NAME_PROPERTY);
        if (servletConnectorName == null)
        {
            connector = (AjaxServletConnector) TransportFactory.getConnectorByProtocol(getConnectorProtocol());
            if (connector == null)
            {
                connector = new AjaxServletConnector();
                try
                {
                    RegistryContext.getRegistry().registerConnector(connector);
                }
                catch (MuleException e)
                {
                    throw new ServletException("Failed to register the AjaxServletConnector", e);
                }
            }
        }
        else
        {
            connector = (AjaxServletConnector) RegistryContext.getRegistry().lookupConnector(servletConnectorName);
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
