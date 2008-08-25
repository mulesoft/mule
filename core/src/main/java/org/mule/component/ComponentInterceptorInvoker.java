/*
 * $Id: InterceptorsInvoker.java 7963 2007-08-21 08:53:15Z dirk.olmes $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.component;

import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.interceptor.Interceptor;
import org.mule.api.interceptor.Invocation;
import org.mule.api.service.Service;

import java.util.List;

/**
 * <code>InterceptorsInvoker</code> is used to trigger an interceptor chain.
 */

public class ComponentInterceptorInvoker implements Invocation
{

    private final AbstractComponent component;
    private final List interceptors;
    private MuleEvent event;
    private int cursor = 0;

    public ComponentInterceptorInvoker(final AbstractComponent component, List interceptors, MuleEvent event)
    {
        this.component = component;
        this.interceptors = interceptors;
        this.interceptors.add(component);
        this.event = event;
    }

    public MuleMessage invoke() throws MuleException
    {
        if (cursor < interceptors.size())
        {
            Interceptor interceptor = (Interceptor) interceptors.get(cursor);
            incCursor();
            return interceptor.intercept(this);
        }
        else
        {
            return getMessage();
        }
    }

    private synchronized void incCursor()
    {
        cursor++;
    }

    public MuleEvent getEvent()
    {
        return event;
    }

    public Service getService()
    {
        return component.getService();
    }

    public MuleMessage getMessage()
    {
        return event.getMessage();
    }

    public synchronized void setMessage(MuleMessage message)
    {
        synchronized (event)
        {
            OptimizedRequestContext.unsafeRewriteEvent(message);
            event = RequestContext.getEvent();
        }
    }

}
