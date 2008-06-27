/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.jetty;

import org.mule.api.endpoint.EndpointException;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.NoReceiverForEndpointException;
import org.mule.transport.http.servlet.MuleReceiverServlet;
import org.mule.util.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;

public class JettyReceiverServlet extends MuleReceiverServlet
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 238326861089137293L;

    private ConcurrentMap receivers = new ConcurrentHashMap(4);

    //@Override
    protected void doInit(ServletConfig servletConfig) throws ServletException
    {
        //do nothing
    }

    //@Override
    protected MessageReceiver getReceiverForURI(HttpServletRequest httpServletRequest)
        throws EndpointException
    {
        MessageReceiver receiver = (MessageReceiver)receivers.get(httpServletRequest.getPathInfo());
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
        String key = receiver.getEndpointURI().getPath();
        if(StringUtils.isEmpty(key))
        {
            key = "/";
        }
        return key;
    }
}
