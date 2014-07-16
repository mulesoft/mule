/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.model.resolvers;

import org.mule.VoidResult;
import org.mule.api.MuleEventContext;
import org.mule.api.model.EntryPointResolver;
import org.mule.api.model.InvocationResult;
import org.mule.api.transformer.TransformerException;
import org.mule.transport.NullPayload;
import org.mule.util.ClassUtils;
import org.mule.util.StringMessageUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Base class for {@link org.mule.api.model.EntryPointResolver}. It provides parameters for
 * detemining if the payload of the message should be transformed first and whether void methods are
 * acceptible. It also provides a method cashe for those resolvers that use reflection to discover methods
 * on the service.
 */
public abstract class AbstractEntryPointResolver implements EntryPointResolver
{
    private static final Log logger = LogFactory.getLog(AbstractEntryPointResolver.class);

    private boolean acceptVoidMethods = false;

    private boolean synchronizeCall = false;

    // @GuardedBy(itself)
    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Method>> methodCache =
        new ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Method>>(4);

    public boolean isAcceptVoidMethods()
    {
        return acceptVoidMethods;
    }

    public void setAcceptVoidMethods(boolean acceptVoidMethods)
    {
        this.acceptVoidMethods = acceptVoidMethods;
    }

    protected ConcurrentHashMap<String, Method> getMethodCache(Object component)
    {
        Class<?> componentClass = component.getClass();
        ConcurrentHashMap<String, Method> cache = methodCache.get(componentClass);
        if (cache == null)
        {
            methodCache.putIfAbsent(componentClass, new ConcurrentHashMap<String, Method>(4));
        }
        return methodCache.get(componentClass);
    }

    protected Method getMethodByName(Object component, String methodName, MuleEventContext context)
    {
        return getMethodCache(component).get(methodName);
    }

    protected Method addMethodByName(Object component, Method method, MuleEventContext context)
    {
        Method previousMethod = getMethodCache(component).putIfAbsent(method.getName(), method);
        return (previousMethod != null ? previousMethod : method);
    }

    protected Method addMethodByArguments(Object component, Method method, Object[] payload)
    {
        Method previousMethod = getMethodCache(component).putIfAbsent(getCacheKeyForPayload(component, payload), method);
        return (previousMethod != null ? previousMethod : method);
    }


    protected Method getMethodByArguments(Object component, Object[] payload)
    {
        Method method = getMethodCache(component).get(getCacheKeyForPayload(component, payload));
        return method;
    }

    protected String getCacheKeyForPayload(Object component, Object[] payload)
    {
        StringBuilder key = new StringBuilder(48);
        for (int i = 0; i < payload.length; i++)
        {
            Object o = payload[i];
            if (o != null)
            {
                key.append(o.getClass().getName());
            }
            else
            {
                key.append("null");
            }
        }
        key.append(".").append(ClassUtils.getClassName(component.getClass()));
        return key.toString();
    }


    protected Object[] getPayloadFromMessage(MuleEventContext context) throws TransformerException
    {
        Object temp = context.getMessage().getPayload();
        if (temp instanceof Object[])
        {
            return (Object[]) temp;
        }
        else if (temp instanceof NullPayload)
        {
            return ClassUtils.NO_ARGS;
        }
        else
        {
            return new Object[]{temp};
        }
    }

    protected InvocationResult invokeMethod(Object component, Method method, Object[] arguments)
            throws InvocationTargetException, IllegalAccessException
    {
        String methodCall = null;

        if (logger.isDebugEnabled())
        {
            methodCall = component.getClass().getName() + "." + method.getName() + "("
                    + StringMessageUtils.toString(ClassUtils.getClassTypes(arguments)) + ")";
            logger.debug("Invoking " + methodCall);
        }

        Object result;

        if(isSynchronizeCall())
        {
            synchronized (component)
            {
                result = method.invoke(component, arguments);
            }
        }
        else
        {
            result = method.invoke(component, arguments);
        }

        if (method.getReturnType().equals(Void.TYPE))
        {
            result = VoidResult.getInstance();
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Result of call " + methodCall + " is " + (result == null ? "null" : "not null"));
        }

        return new InvocationResult(this, result, method);
    }

    public boolean isSynchronizeCall()
    {
        return synchronizeCall;
    }

    public void setSynchronizeCall(boolean synchronizeCall)
    {
        this.synchronizeCall = synchronizeCall;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("AbstractEntryPointResolver");
        sb.append(", acceptVoidMethods=").append(acceptVoidMethods);
        sb.append('}');
        return sb.toString();
    }
}
