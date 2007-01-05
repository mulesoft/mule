/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.servlet;

import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.umo.UMOException;

import java.util.Map;

/**
 * <code>ServletConnector</code> is a channel adapter between Mule and a servlet
 * engine. It allows the MUleReceiverServlet to look up components interested in
 * requests it receives via the servlet container.
 * 
 * @see MuleReceiverServlet
 */

public class ServletConnector extends AbstractServiceEnabledConnector
{
    // The real URL that the servlet container is bound on.
    // If this is not set the wsdl may not be generated correctly
    protected String servletUrl;

    public ServletConnector()
    {
        super();
        registerSupportedProtocol("http");
        registerSupportedProtocol("https");
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

    protected void doStart() throws UMOException
    {
        // template method
    }

    protected void doStop() throws UMOException
    {
        // template method
    }

    public String getProtocol()
    {
        return "servlet";
    }

    public Map getReceivers()
    {
        return receivers;
    }

    public String getServletUrl()
    {
        return servletUrl;
    }

    public void setServletUrl(String servletUrl)
    {
        this.servletUrl = servletUrl;
    }

}
