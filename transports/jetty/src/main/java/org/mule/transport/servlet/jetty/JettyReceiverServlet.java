/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.NoReceiverForEndpointException;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.servlet.MuleReceiverServlet;
import org.mule.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * TODO: MULE-7690 Replace servlets with handlers in the implementation of inbound endpoints in the Jetty transport
 */
public class JettyReceiverServlet extends MuleReceiverServlet
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 238326861089137293L;

    private ConcurrentMap receivers = new ConcurrentHashMap(4);

    public JettyReceiverServlet(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    protected MessageReceiver getReceiverForURI(HttpServletRequest httpServletRequest)
        throws EndpointException
    {
        String key = httpServletRequest.getPathInfo();
        if(StringUtils.EMPTY.equals(key))
        {
            key = httpServletRequest.getContextPath();
        }

        MessageReceiver receiver = (MessageReceiver)receivers.get(key);
        if (receiver == null)
        {
            receiver = HttpConnector.findReceiverByStem(receivers, key);
        }
        
        if (receiver == null)
        {
            throw new NoReceiverForEndpointException(httpServletRequest.getPathInfo());
        }
        
        return receiver;
    }

    void addReceiver(MessageReceiver receiver)
    {
        receivers.putIfAbsent(getReceiverKey(receiver), receiver);
    }

    boolean removeReceiver(MessageReceiver receiver)
    {
        return receivers.remove(getReceiverKey(receiver), receiver);
    }

    protected String getReceiverKey(MessageReceiver receiver)
    {
        String key = receiver.getEndpoint().getEndpointURI().getPath();
        if(StringUtils.isEmpty(key))
        {
            key = "/";
        }
        return key;
    }

    @Override
    protected void processHttpRequest(HttpServletRequest request, HttpServletResponse response, MessageReceiver receiver) throws Exception
    {
        ((JettyHttpMessageReceiver)receiver).processMessage(request, response);
    }

}
