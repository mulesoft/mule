/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.component;

import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.component.InterfaceBinding;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.MessagingExceptionHandlerAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;

import java.lang.reflect.Proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultInterfaceBinding implements InterfaceBinding, MessagingExceptionHandlerAware, Initialisable
{
    protected static final Log logger = LogFactory.getLog(DefaultInterfaceBinding.class);

    private Class<?> interfaceClass;

    private String methodName;

    private MessagingExceptionHandler messagingExceptionHandler;

    // The router used to actually dispatch the message
    protected OutboundEndpoint endpoint;

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        OptimizedRequestContext.unsafeRewriteEvent(event.getMessage());
        return endpoint.process(RequestContext.getEvent());
    }

    public void setInterface(Class<?> interfaceClass)
    {
        this.interfaceClass = interfaceClass;
    }

    public Class<?> getInterface()
    {
        return interfaceClass;
    }

    public String getMethod()
    {
        return methodName;
    }

    public void setMethod(String methodName)
    {
        this.methodName = methodName;
    }

    public Object createProxy(Object target)
    {
        try
        {
            Object proxy = Proxy.newProxyInstance(getInterface().getClassLoader(), new Class[]{getInterface()},
                    new BindingInvocationHandler(this));
            if (logger.isDebugEnabled())
            {
                logger.debug("Have proxy?: " + (null != proxy));
            }
            return proxy;

        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(CoreMessages.failedToCreateProxyFor(target), e);
        }
    }

    public void setEndpoint(ImmutableEndpoint e) throws MuleException
    {
        if (e instanceof OutboundEndpoint)
        {
            endpoint = (OutboundEndpoint) e;
        }
        else
        {
            throw new IllegalArgumentException("An outbound endpoint is required for Interface binding");
        }
    }

    public Class<?> getInterfaceClass()
    {
        return interfaceClass;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("DefaultInterfaceBinding");
        sb.append("{method='").append(methodName).append('\'');
        sb.append(", interface=").append(interfaceClass);
        sb.append('}');
        return sb.toString();
    }

    public ImmutableEndpoint getEndpoint()
    {
        if (endpoint != null)
        {
            return endpoint;
        }
        else
        {
            return null;
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        endpoint.setMessagingExceptionHandler(messagingExceptionHandler);
    }

    @Override
    public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler)
    {
        this.messagingExceptionHandler = messagingExceptionHandler;
    }
}
