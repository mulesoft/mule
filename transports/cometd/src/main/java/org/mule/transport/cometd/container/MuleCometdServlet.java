/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.cometd.container;

import org.mule.transport.servlet.AbstractReceiverServlet;
import org.mule.transport.servlet.i18n.ServletMessages;
import org.mule.transport.service.TransportFactory;
import org.mule.transport.cometd.i18n.CometdMessages;
import org.mule.transport.AbstractConnector;
import org.mule.RegistryContext;
import org.mule.api.MuleException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.mortbay.cometd.continuation.ContinuationCometdServlet;

/**
 * Wraps the {@link org.mortbay.cometd.continuation.ContinuationCometdServlet} servlet and binds the Bayeux object to
 * the Mule {@link org.mule.transport.cometd.container.CometdServletConnector}.
 */
public class MuleCometdServlet extends ContinuationCometdServlet
{
     /** The name of the cometd connector to use with this Servlet */
    public static final String COMETD_CONNECTOR_NAME_PROPERTY = "org.mule.cometd.connector.name";

    protected CometdServletConnector connector = null;

    @Override
    public void init() throws ServletException
    {
        super.init();
        String servletConnectorName = getServletConfig().getInitParameter(COMETD_CONNECTOR_NAME_PROPERTY);
        if (servletConnectorName == null)
        {
            connector = (CometdServletConnector) TransportFactory.getConnectorByProtocol(CometdServletConnector.PROTOCOL);
            if (connector == null)
            {
                connector = new CometdServletConnector();
                try
                {
                    RegistryContext.getRegistry().registerConnector(connector);
                }
                catch (MuleException e)
                {
                    throw new ServletException("Failed to register the CometdServletConnector", e);
                }
                //throw new ServletException(CometdMessages.noConnectorForProtocol(CometdServletConnector.PROTOCOL).toString());
            }
        }
        else
        {
            connector = (CometdServletConnector) RegistryContext.getRegistry().lookupConnector(servletConnectorName);
            if (connector == null)
            {
                throw new ServletException(CometdMessages.noCometdConnectorWithName(servletConnectorName, COMETD_CONNECTOR_NAME_PROPERTY).toString());
            }
        }
        connector.setBayeux(getBayeux());
    }
    
}
