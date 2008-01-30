/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.service.ServiceException;
import org.mule.transport.NullPayload;

import java.lang.reflect.Method;

import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.BindingOperationInfo;

/**
 * Invokes a Mule Service via a CXF binding.
 */
public class MuleInvoker implements Invoker
{
    private final CxfMessageReceiver receiver;
    private final boolean synchronous;

    public MuleInvoker(CxfMessageReceiver receiver, boolean synchronous)
    {
        this.receiver = receiver;
        this.synchronous = synchronous;
    }

    public Object invoke(Exchange exchange, Object o)
    {
        BindingOperationInfo bop = exchange.get(BindingOperationInfo.class);
        MethodDispatcher md = (MethodDispatcher) exchange.get(Service.class).get(
            MethodDispatcher.class.getName());
        Method m = md.getMethod(bop);

        MuleMessage message = null;
        try
        {
            CxfMessageAdapter messageAdapter = (CxfMessageAdapter) receiver.getConnector().getMessageAdapter(
                exchange.getInMessage());

            if (!receiver.isBridge())
            {
                messageAdapter.setProperty(MuleProperties.MULE_METHOD_PROPERTY, m);
            }

            message = receiver.routeMessage(new DefaultMuleMessage(messageAdapter), synchronous);
        }
        catch (MuleException e)
        {
            throw new Fault(e);
        }

        if (message != null)
        {
            if (message.getExceptionPayload() != null)
            {
                Throwable cause = message.getExceptionPayload().getException();
                if (cause instanceof ServiceException)
                {
                    cause = cause.getCause();
                }

                exchange.getInMessage().put(FaultMode.class, FaultMode.UNCHECKED_APPLICATION_FAULT);
                if (cause instanceof Fault)
                {
                    throw (Fault) cause;
                }

                throw new Fault(cause);
            }
            else if (message.getPayload() instanceof NullPayload)
            {
                return null;
            }
            else
            {
                return new Object[]{message.getPayload()};
            }
        }
        else
        {
            return null;
        }
    }

    public ImmutableEndpoint getEndpoint()
    {
        return receiver.getEndpoint();
    }
}
