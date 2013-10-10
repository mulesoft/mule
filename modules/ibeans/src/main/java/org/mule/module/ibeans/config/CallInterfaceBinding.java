/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.config;

import org.mule.DefaultMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.MuleSession;
import org.mule.api.component.InterfaceBinding;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.component.BindingInvocationHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.management.stats.RouterStatistics;
import org.mule.session.DefaultMuleSession;

import java.lang.reflect.Proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An instance of a binding that matches iBean method name with an endpoint to invoke.
 * 
 * Each method annotated with {@link org.ibeans.annotation.Call} or {@link org.ibeans.annotation.Template} has an associated
 * component binding associated with it.
 */
public class CallInterfaceBinding implements InterfaceBinding, MessageProcessor
{
    protected static final Log logger = LogFactory.getLog(CallInterfaceBinding.class);

    private Class<?> interfaceClass;

    private String methodName;

    // The router used to actually dispatch the message
    protected OutboundEndpoint endpoint;
    private FlowConstruct flow;
    private RouterStatistics routerStatistics;


    public CallInterfaceBinding(FlowConstruct flow)
    {
        routerStatistics = new RouterStatistics(RouterStatistics.TYPE_BINDING);
        this.flow = flow;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return endpoint.process(event);
    }

    public MuleMessage route(MuleMessage message, MuleSession session) throws MuleException
    {
        //Work around for allowing the MuleCallAnnotationHandler to invoke the binding directly without having
        //to know about the flow and create a session
        if(session==null)
        {
            session = new DefaultMuleSession(flow, message.getMuleContext());
        }

        MuleEvent result = process(new DefaultMuleEvent(message, endpoint, session));
        if (result != null)
        {
            return result.getMessage();
        }
        else
        {
            return null;
        }
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

    public void setEndpoint(ImmutableEndpoint e)
    {
        if (e instanceof OutboundEndpoint)
        {
            endpoint = (OutboundEndpoint)e;
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
        final StringBuffer sb = new StringBuffer();
        sb.append("DefaultInterfaceBinding");
        sb.append("{method='").append(methodName).append('\'');
        sb.append(", interface=").append(interfaceClass);
        sb.append('}');
        return sb.toString();
    }

    public ImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }
}
