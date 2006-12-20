/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.glue;

import java.util.HashMap;
import java.util.Map;

import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.ReceiveException;

import electric.glue.context.ProxyContext;
import electric.glue.context.ThreadContext;
import electric.proxy.IProxy;
import electric.registry.Registry;

/**
 * <code>GlueMessageDispatcher</code> will make web services calls using the Glue
 * invoking mechanism.
 */

public class GlueMessageDispatcher extends AbstractMessageDispatcher
{
    protected IProxy proxy = null;

    public GlueMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    protected void doConnect() throws Exception
    {
        if (proxy == null)
        {
            String bindAddress = endpoint.getEndpointURI().getAddress();
            String method = (String)endpoint.getProperty(MuleProperties.MULE_METHOD_PROPERTY);
            if (bindAddress.indexOf(".wsdl") == -1 && method != null)
            {
                bindAddress = bindAddress.replaceAll("/" + method, ".wsdl/" + method);
            }
            int i = bindAddress.indexOf("?");
            if (i > -1)
            {
                bindAddress = bindAddress.substring(0, i);
            }

            // add credentials to the request
            if (endpoint.getEndpointURI().getUsername() != null)
            {
                ProxyContext context = new ProxyContext();
                context.setAuthUser(endpoint.getEndpointURI().getUsername());
                context.setAuthPassword(new String(endpoint.getEndpointURI().getPassword()));
                proxy = Registry.bind(bindAddress, context);
            }
            else
            {
                proxy = Registry.bind(bindAddress);
            }
        }
    }

    protected void doDisconnect() throws Exception
    {
        proxy = null;
    }

    protected void doDispatch(UMOEvent event) throws Exception
    {
        doSend(event);
    }

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {

        String method = event.getMessage().getStringProperty(MuleProperties.MULE_METHOD_PROPERTY, null);
        if (method == null)
        {
            method = (String)event.getEndpoint().getProperty(MuleProperties.MULE_METHOD_PROPERTY);
        }
        setContext(event);

        Object payload = event.getTransformedMessage();
        Object[] args;
        if (payload instanceof Object[])
        {
            args = (Object[])payload;
        }
        else
        {
            args = new Object[]{payload};
        }
        if (event.getMessage().getReplyTo() != null)
        {
            ThreadContext.setProperty(MuleProperties.MULE_REPLY_TO_PROPERTY, event.getMessage().getReplyTo());
        }
        if (event.getMessage().getCorrelationId() != null)
        {
            ThreadContext.setProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY, event.getMessage()
                .getCorrelationId());
        }
        try
        {
            Object result = proxy.invoke(method, args);
            if (result == null)
            {
                return null;
            }
            else
            {
                return new MuleMessage(result);
            }
        }
        catch (Throwable t)
        {
            throw new DispatchException(event.getMessage(), event.getEndpoint(), t);
        }
    }

    /**
     * Make a specific request to the underlying transport
     * 
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(long timeout) throws Exception
    {
        Map props = new HashMap();
        props.putAll(endpoint.getProperties());
        String method = (String)props.remove(MuleProperties.MULE_METHOD_PROPERTY);
        try
        {
            Object result = proxy.invoke(method, props.values().toArray());
            return new MuleMessage(result);
        }
        catch (Throwable t)
        {
            throw new ReceiveException(endpoint, timeout, t);
        }
    }

    protected void doDispose()
    {
        // template method
    }

    protected String getMethod(String endpoint) throws MalformedEndpointException
    {
        int i = endpoint.lastIndexOf("/");
        String method = endpoint.substring(i + 1);
        if (method.indexOf(".wsdl") != -1)
        {
            throw new MalformedEndpointException(
                "Soap url must contain method to invoke as a param [method=X] or as the last path element");
        }
        else
        {
            return method;
            // endpointUri = endpointUri.substring(0, endpointUri.length() -
            // (method.length() + 1));
        }
    }

    protected void setContext(UMOEvent event)
    {
        Object replyTo = event.getMessage().getReplyTo();
        if (replyTo != null)
        {
            ThreadContext.setProperty(MuleProperties.MULE_REPLY_TO_PROPERTY, replyTo);
        }

        String correlationId = event.getMessage().getCorrelationId();
        if (replyTo != null)
        {
            ThreadContext.setProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY, correlationId);
        }

        int value = event.getMessage().getCorrelationSequence();
        if (value > 0)
        {
            ThreadContext.setProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY,
                String.valueOf(value));
        }

        value = event.getMessage().getCorrelationGroupSize();
        if (value > 0)
        {
            ThreadContext.setProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY,
                String.valueOf(value));
        }
    }
}
