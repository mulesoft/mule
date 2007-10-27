/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.model.resolvers;

import org.mule.impl.VoidResult;
import org.mule.providers.NullPayload;
import org.mule.umo.UMOEventContext;
import org.mule.umo.model.InvocationResult;
import org.mule.umo.model.UMOEntryPointResolver;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.ClassUtils;
import org.mule.util.StringMessageUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Base class for {@link org.mule.umo.model.UMOEntryPointResolver}. It provides parameters for
 * detemining if the payload of the message should be transformed first and whether void methods are
 * acceptible. It also provides a method cashe for those resolvers that use reflection to discover methods
 * on the component.
 */
public abstract class AbstractEntryPointResolver implements UMOEntryPointResolver
{
    /** logger used by this class */
    protected transient final Log logger = LogFactory.getLog(getClass());

    private boolean transformFirst = true;

    private boolean acceptVoidMethods = false;


    // @GuardedBy(itself)
    protected final ConcurrentHashMap methodCache = new ConcurrentHashMap(4);

    public boolean isTransformFirst()
    {
        return transformFirst;
    }

    public void setTransformFirst(boolean transformFirst)
    {
        this.transformFirst = transformFirst;
    }

    public boolean isAcceptVoidMethods()
    {
        return acceptVoidMethods;
    }

    public void setAcceptVoidMethods(boolean acceptVoidMethods)
    {
        this.acceptVoidMethods = acceptVoidMethods;
    }

    protected Method getMethodByName(String methodName, UMOEventContext context)
    {
        StringBuffer key = new StringBuffer(24).append(context.getComponent().getName())
                .append(".").append(methodName);
        Method method = (Method) methodCache.get(key);
        return method;
    }

    protected Method addMethodByName(Method method, UMOEventContext context)
    {
        StringBuffer key = new StringBuffer(24).append(context.getComponent().getName())
                .append(".").append(method.getName());
        Method previousMethod = (Method) methodCache.putIfAbsent(key, method);
        return (previousMethod != null ? previousMethod : method);
    }

    protected Method addMethodByArguments(Object component, Method method, Object[] payload)
    {
        Method previousMethod = (Method) methodCache.putIfAbsent(getCacheKeyForPayload(component, payload), method);
        return (previousMethod != null ? previousMethod : method);
    }


    protected Method getMethodByArguments(Object component, Object[] payload)
    {
        Method method = (Method) methodCache.get(getCacheKeyForPayload(component, payload));
        return method;
    }

    protected String getCacheKeyForPayload(Object component, Object[] payload)
    {
        StringBuffer key = new StringBuffer(48);
        for (int i = 0; i < payload.length; i++)
        {
            Object o = payload[i];
            key.append(o.getClass().getName());
        }
        key.append(".").append(ClassUtils.getClassName(component.getClass()));
        return key.toString();
    }


    protected Object[] getPayloadFromMessage(UMOEventContext context) throws TransformerException
    {
        Object temp;
        if (isTransformFirst())
        {
            temp = context.getTransformedMessage();
        }
        else
        {
            temp = context.getMessage().getPayload();
        }
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

        Object result = method.invoke(component, arguments);
        if (method.getReturnType().equals(Void.TYPE))
        {
            result = VoidResult.getInstance();
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Result of call " + methodCall + " is " + (result == null ? "null" : "not null"));
        }

        return new InvocationResult(result, method);
    }


    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append("AbstractEntryPointResolver");
        sb.append("{transformFirst=").append(transformFirst);
        sb.append(", acceptVoidMethods=").append(acceptVoidMethods);
        sb.append('}');
        return sb.toString();
    }
}
