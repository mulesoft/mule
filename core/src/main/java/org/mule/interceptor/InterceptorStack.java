/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.interceptor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.interceptor.Interceptor;
import org.mule.api.interceptor.Invocation;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;

import java.util.Iterator;
import java.util.List;

/**
 * Maintains a list of interceptors that can be applied to components.
 */
public class InterceptorStack implements Interceptor, Initialisable, Disposable
{

    private List interceptors;

    public InterceptorStack()
    {
        // For spring
    }

    public InterceptorStack(List interceptors)
    {
        this.interceptors = interceptors;
    }

    public MuleMessage intercept(Invocation invocation) throws MuleException
    {
        return new Invoc(invocation).invoke();
    }

    private class Invoc implements Invocation
    {
        private int cursor = 0;
        private Invocation invocation;

        public Invoc(Invocation invocation)
        {
            this.invocation = invocation;
        }

        public MuleMessage invoke() throws MuleException
        {
            if (interceptors != null && cursor < interceptors.size())
            {
                Interceptor interceptor = (Interceptor) interceptors.get(cursor);
                cursor++;
                setMessage(interceptor.intercept(this));
            }
            else
            {
                invocation.setMessage(getMessage());
                setMessage(invocation.invoke());
            }
            return getMessage();
        }

        public MuleEvent getEvent()
        {
            return invocation.getEvent();
        }

        public MuleMessage getMessage()
        {
            return invocation.getMessage();
        }

        public Service getService()
        {
            return invocation.getService();
        }

        public void setMessage(MuleMessage message)
        {
            invocation.setMessage(message);
        }
    }

    public List getInterceptors()
    {
        return interceptors;
    }

    public void setInterceptors(List interceptors)
    {
        this.interceptors = interceptors;
    }

    public void initialise() throws InitialisationException
    {
        for (Iterator it = interceptors.iterator(); it.hasNext();)
        {
            Interceptor interceptor = (Interceptor) it.next();
            if (interceptor instanceof Initialisable)
            {
                ((Initialisable) interceptor).initialise();
            }
        }
    }

    public void dispose()
    {
        for (Iterator it = interceptors.iterator(); it.hasNext();)
        {
            Interceptor interceptor = (Interceptor) it.next();
            if (interceptor instanceof Disposable)
            {
                ((Disposable) interceptor).dispose();
            }
        }
    }

    // @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((interceptors == null) ? 0 : interceptors.hashCode());
        return result;
    }

    // @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        InterceptorStack other = (InterceptorStack) obj;
        if (interceptors == null)
        {
            if (other.interceptors != null) return false;
        }
        else if (!interceptors.equals(other.interceptors)) return false;
        return true;
    }

}
