/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct.builder;

import java.beans.ExceptionListener;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.service.DefaultServiceExceptionStrategy;

public abstract class AbstractFlowConstructBuilder<T extends AbstractFlowConstructBuilder, F extends AbstractFlowConstruct>
{
    protected static final DefaultServiceExceptionStrategy DEFAULT_SERVICE_EXCEPTION_STRATEGY = new DefaultServiceExceptionStrategy();

    protected String name;
    protected String address;
    protected EndpointBuilder endpointBuilder;
    protected ExceptionListener exceptionListener;

    public T named(String name)
    {
        this.name = name;
        return (T) this;
    }

    public T withExceptionListener(ExceptionListener exceptionListener)
    {
        this.exceptionListener = exceptionListener;
        return (T) this;
    }

    public T receivingOn(EndpointBuilder endpointBuilder)
    {
        this.endpointBuilder = endpointBuilder;
        return (T) this;
    }

    public T receivingOn(String address)
    {
        this.address = address;
        return (T) this;
    }

    public F in(MuleContext muleContext) throws MuleException
    {
        F flowConstruct = buildFlowConstruct(muleContext);
        addExceptionListener(flowConstruct);
        return flowConstruct;
    }

    protected abstract F buildFlowConstruct(MuleContext muleContext) throws MuleException;

    protected void addExceptionListener(AbstractFlowConstruct flowConstruct)
    {
        if (exceptionListener != null)
        {
            flowConstruct.setExceptionListener(exceptionListener);
        }
        else
        {
            flowConstruct.setExceptionListener(DEFAULT_SERVICE_EXCEPTION_STRATEGY);
        }
    }
}
