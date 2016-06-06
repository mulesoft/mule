/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.component;

import org.mule.compatibility.core.api.component.InterfaceBinding;
import org.mule.compatibility.core.config.i18n.TransportCoreMessages;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.StringMessageUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO MULE-9449 Remove Java Component Bindings
 */
public class BindingInvocationHandler implements InvocationHandler
{

    public static final String DEFAULT_METHOD_NAME_TOKEN = "default";

    protected static Log logger = LogFactory.getLog(BindingInvocationHandler.class);

    protected Map<String, InterfaceBinding> routers = null;

    protected MuleContext muleContext;

    public BindingInvocationHandler(InterfaceBinding router)
    {
        this.routers = new ConcurrentHashMap<String, InterfaceBinding>();
        addRouterForInterface(router);
    }

    public void addRouterForInterface(InterfaceBinding router)
    {
        if (router.getMethod() == null)
        {
            if (routers.size() == 0)
            {
                routers.put(DEFAULT_METHOD_NAME_TOKEN, router);
            }
            else
            {
                throw new IllegalArgumentException(TransportCoreMessages.mustSetMethodNamesOnBinding().getMessage());
            }
        }
        else
        {
            routers.put(router.getMethod(), router);
        }
        muleContext = router.getEndpoint().getMuleContext();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (method.getName().equals("toString"))
        {
            return toString();
        }

        MuleMessage message = createMuleMessage(args);

        InterfaceBinding router = routers.get(method.getName());
        if (router == null)
        {
            router = routers.get(DEFAULT_METHOD_NAME_TOKEN);
        }

        if (router == null)
        {
            throw new IllegalArgumentException(TransportCoreMessages.cannotFindBindingForMethod(method.getName()).toString());
        }

        MuleEvent currentEvent = RequestContext.getEvent();
        MuleEvent replyEvent = router.process(new DefaultMuleEvent(message,currentEvent));

        if (replyEvent != null && !VoidMuleEvent.getInstance().equals(replyEvent)
            && replyEvent.getMessage() != null)
        {
            MuleMessage reply = replyEvent.getMessage();
            if (reply.getExceptionPayload() != null)
            {
                throw findDeclaredMethodException(method, reply.getExceptionPayload().getException());
            }
            else
            {
                return determineReply(reply, method);
            }
        }
        else
        {
            return null;
        }
    }

    private MuleMessage createMuleMessage(Object[] args)
    {
        if (args == null)
        {
            return new DefaultMuleMessage(NullPayload.getInstance(), muleContext);
        }
        else if (args.length == 1)
        {
            return new DefaultMuleMessage(args[0], muleContext);
        }
        else
        {
            return new DefaultMuleMessage(args, muleContext);
        }
    }

    /**
     * Return the causing exception instead of the general "container" exception (typically
     * UndeclaredThrowableException) if the cause is known and the type matches one of the
     * exceptions declared in the given method's "throws" clause.
     */
    private Throwable findDeclaredMethodException(Method method, Throwable throwable) throws Throwable
    {
        Throwable cause = throwable.getCause();
        if (cause != null)
        {
            // Try to find a matching exception type from the method's "throws" clause, and if so
            // return that exception.
            Class<?>[] exceptions = method.getExceptionTypes();
            for (int i = 0; i < exceptions.length; i++)
            {
                if (cause.getClass().equals(exceptions[i]))
                {
                    return cause;
                }
            }
        }

        return throwable;
    }

    private Object determineReply(MuleMessage reply, Method bindingMethod)
    {
        if (MuleMessage.class.isAssignableFrom(bindingMethod.getReturnType()))
        {
            return reply;
        }
        else if (reply.getPayload() == NullPayload.getInstance() && !bindingMethod.getReturnType().isInstance(reply.getPayload()))
        {
            return null;
        }
        else
        {
            return reply.getPayload();
        }
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("BindingInvocation");
        sb.append("{routers='").append(StringMessageUtils.toString(routers));
        sb.append('}');
        return sb.toString();
    }

}
