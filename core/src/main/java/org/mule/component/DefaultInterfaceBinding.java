/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.OutboundRouter;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.outbound.OutboundPassThroughRouter;

import java.lang.reflect.Proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultInterfaceBinding implements InterfaceBinding
{
    protected static final Log logger = LogFactory.getLog(DefaultInterfaceBinding.class);

    private Class<?> interfaceClass;

    private String methodName;

    // The router used to actually dispatch the message
    protected OutboundRouter outboundRouter;

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        OptimizedRequestContext.unsafeRewriteEvent(event.getMessage());
        return outboundRouter.process(RequestContext.getEvent());
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
            outboundRouter = new OutboundPassThroughRouter();
            outboundRouter.addRoute((OutboundEndpoint) e);
            outboundRouter.setTransactionConfig(e.getTransactionConfig());
            outboundRouter.setMuleContext(e.getMuleContext());
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
        if (outboundRouter != null)
        {
            MessageProcessor target = outboundRouter.getRoutes().get(0);
            return target instanceof ImmutableEndpoint ? (ImmutableEndpoint) target : null;
        }
        else
        {
            return null;
        }
    }
}
