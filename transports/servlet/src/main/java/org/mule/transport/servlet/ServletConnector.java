/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet;

import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.transport.AbstractConnector;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpsConnector;

import java.util.Map;

/**
 * <code>ServletConnector</code> is a channel adapter between Mule and a servlet
 * engine. It allows the MuleReceiverServlet to look up components interested in
 * requests it receives via the servlet container.
 * 
 * @see MuleReceiverServlet
 */

public class ServletConnector extends AbstractConnector
{

    public static final String SERVLET = "servlet";

    // The real URL that the servlet container is bound on.
    // If this is not set the wsdl may not be generated correctly
    protected String servletUrl;

    public ServletConnector()
    {
        super();
        registerSupportedProtocol(HttpConnector.HTTP);
        registerSupportedProtocol(HttpsConnector.HTTPS);
    }


    protected void doInitialise() throws InitialisationException
    {
        // template method, nothing to do
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

    protected void doStart() throws MuleException
    {
        // template method
    }

    protected void doStop() throws MuleException
    {
        // template method
    }

    public String getProtocol()
    {
        return SERVLET;
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

    protected Object getReceiverKey(Service service, InboundEndpoint endpoint)
    {
        EndpointURI uri = endpoint.getEndpointURI();
        return uri.getAddress();
    }
}
