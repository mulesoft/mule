/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.jetty;

import org.mule.api.endpoint.EndpointException;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.NoReceiverForEndpointException;
import org.mule.transport.http.HttpMessageReceiver;
import org.mule.transport.servlet.MuleReceiverServlet;
import org.mule.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;

public class JettyReceiverServlet extends MuleReceiverServlet
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 238326861089137293L;

    private ConcurrentMap receivers = new ConcurrentHashMap(4);

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
            receiver = HttpMessageReceiver.findReceiverByStem(receivers, key);
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
}
