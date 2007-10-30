/*
 * $Id: MuleInvoker.java 3903 2006-11-17 21:05:19Z holger $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.providers.NullPayload;
import org.mule.umo.ComponentException;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

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

        UMOMessage message = null;
        try
        {
            CxfMessageAdapter messageAdapter = (CxfMessageAdapter) receiver.getConnector().getMessageAdapter(
                exchange.getInMessage());

            if (!receiver.isBridge())
            {
                messageAdapter.setProperty(MuleProperties.MULE_METHOD_PROPERTY, m);
            }

            message = receiver.routeMessage(new MuleMessage(messageAdapter), synchronous);
        }
        catch (UMOException e)
        {
            throw new Fault(e);
        }

        if (message != null)
        {
            if (message.getExceptionPayload() != null)
            {
                Throwable cause = message.getExceptionPayload().getException();
                if (cause instanceof ComponentException)
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

    public UMOImmutableEndpoint getEndpoint()
    {
        return receiver.getEndpoint();
    }
}
