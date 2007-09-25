/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.jetty;

import org.mule.providers.http.i18n.HttpMessages;
import org.mule.providers.http.servlet.MuleReceiverServlet;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.provider.UMOMessageReceiver;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public class JettyReceiverServlet extends MuleReceiverServlet
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 238326861089137293L;

    private UMOMessageReceiver receiver;

    //@Override
    protected void doInit(ServletConfig servletConfig) throws ServletException
    {
        final ServletContext servletContext = servletConfig.getServletContext();
        synchronized (servletContext)
        {
            receiver = (UMOMessageReceiver) servletContext.getAttribute("messageReceiver");
        }
        if (receiver == null)
        {
            throw new ServletException(HttpMessages.receiverPropertyNotSet().toString());
        }
    }

    //@Override
    protected UMOMessageReceiver getReceiverForURI(HttpServletRequest httpServletRequest)
        throws EndpointException
    {
        return receiver;
    }
}
