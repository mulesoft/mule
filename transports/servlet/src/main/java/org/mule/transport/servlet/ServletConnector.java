/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.MessageReceiver;
import org.mule.transport.AbstractConnector;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpsConnector;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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

    /**
     * This property name is used to store the session id {@link HttpServletRequest} to
     * the {@link MuleMessage}
     */
    public static final String SESSION_ID_PROPERTY_KEY = MuleProperties.PROPERTY_PREFIX + "SESSION_ID";

    /**
     * This property name is used to store the character encoding of the {@link HttpServletRequest} to
     * the {@link MuleMessage}
     */
    public static final String CHARACTER_ENCODING_PROPERTY_KEY = MuleProperties.PROPERTY_PREFIX + "CHARACTER_ENCODING";

    /**
     * This property name is used to store the content type of the {@link HttpServletRequest} to
     * the {@link MuleMessage}
     */
    public static final String CONTENT_TYPE_PROPERTY_KEY = MuleProperties.PROPERTY_PREFIX + "CONTENT_TYPE";

    /**
     * This prefix is used to store parameters from the incoming {@link HttpServletRequest} to
     * the {@link MuleMessage}.
     */
    public static final String PARAMETER_PROPERTY_PREFIX = "REQUEST_PARAMETER_";
    
    /**
     * This property name is used to store a {@link Map} containing all request parameters to the
     * {@link MuleMessage}.
     */
    public static final String PARAMETER_MAP_PROPERTY_KEY = "request.parameters";

    // The real URL that the servlet container is bound on.
    // If this is not set the wsdl may not be generated correctly
    protected String servletUrl;
    
    private boolean useCachedHttpServletRequest = false;

    public ServletConnector(MuleContext context)
    {
        super(context);
        registerSupportedProtocol(HttpConnector.HTTP);
        registerSupportedProtocol(HttpsConnector.HTTPS);
    }


    @Override
    protected void doInitialise() throws InitialisationException
    {
        // template method, nothing to do
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

    @Override
    protected void doConnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doStart() throws MuleException
    {
        // template method
    }

    @Override
    protected void doStop() throws MuleException
    {
        // template method
    }

    public String getProtocol()
    {
        return SERVLET;
    }

    @Override
    public Map<Object, MessageReceiver> getReceivers()
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

    @Override
    protected Object getReceiverKey(FlowConstruct flowConstruct, InboundEndpoint endpoint)
    {
        EndpointURI uri = endpoint.getEndpointURI();
        return uri.getAddress();
    }

    public boolean isUseCachedHttpServletRequest()
    {
        return useCachedHttpServletRequest;
    }

    public void setUseCachedHttpServletRequest(boolean useCachedHttpServletRequest)
    {
        this.useCachedHttpServletRequest = useCachedHttpServletRequest;
    }
}
