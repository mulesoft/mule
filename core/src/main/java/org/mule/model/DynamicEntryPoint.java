/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.providers.NullPayload;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.model.UMOEntryPoint;
import org.mule.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>DynamicEntryPoint</code> is used to detemine the entry point on a bean
 * after an event has been received for it. The entrypoint is then discovered using
 * the event payload type as the argument. An entry point will try and be matched for
 * different argument types so it's possible to have multiple entry points on a
 * single component.
 */

public class DynamicEntryPoint implements UMOEntryPoint
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(DynamicEntryPoint.class);

    private Map entryPoints = new HashMap();
    private Method currentMethod;

    // we don't want to match these methods when looking for a service method to
    // invoke
    protected String[] ignoreMethods = new String[]{"equals", "getInvocationHandler"};

    public Class[] getParameterTypes()
    {
        if (currentMethod == null)
        {
            return null;
        }
        return currentMethod.getParameterTypes();
    }

    public synchronized Object invoke(Object component, UMOEventContext context) throws Exception
    {
        Object payload = null;
        Method method = null;

        // Transports such as Soap need he method property to be ignorred
        Boolean ignoreMethod = (Boolean)context.getMessage().removeProperty(
            MuleProperties.MULE_IGNORE_METHOD_PROPERTY);
        boolean ignore = (ignoreMethod == null ? false : ignoreMethod.booleanValue());

        if (!ignore)
        {
            // Check for method override and remove it from the event
            Object methodOverride = context.getMessage().removeProperty(MuleProperties.MULE_METHOD_PROPERTY);
            if (methodOverride instanceof Method)
            {
                method = (Method)methodOverride;
            }
            else if (methodOverride != null)
            {
                payload = context.getTransformedMessage();
                // Get the method that matches the method name with the current
                // argument types
                method = ClassUtils.getMethod(methodOverride.toString(), ClassUtils.getClassTypes(payload),
                    component.getClass());
                validateMethod(component, method, methodOverride.toString());
            }
        }

        if (method == null)
        {
            if (component instanceof Callable)
            {
                method = Callable.class.getMethods()[0];
                payload = context;
            }
            if (method == null)
            {
                method = getMethod(component, context);
                if (method == null)
                {
                    payload = context.getTransformedMessage();
                    method = getMethod(component, payload);
                    if (method != null)
                    {
                        RequestContext.rewriteEvent(new MuleMessage(payload, context.getMessage()));
                    }
                }
                else
                {
                    payload = context;
                }
            }
        }
        if (method != null)
        {
            validateMethod(component, method, method.getName());

            currentMethod = method;
            if (payload == null)
            {
                payload = context.getTransformedMessage();
                RequestContext.rewriteEvent(new MuleMessage(payload, context.getMessage()));
            }
            return invokeCurrent(component, payload);
        }

        // Are any methods on the component accepting an context?
        List methods = ClassUtils.getSatisfiableMethods(component.getClass(),
            ClassUtils.getClassTypes(context), true, false, ignoreMethods);
        if (methods.size() > 1)
        {
            TooManySatisfiableMethodsException tmsmex = new TooManySatisfiableMethodsException(
                component.getClass());
            throw new InvocationTargetException(tmsmex, "There must be only one method accepting "
                                                        + context.getClass().getName() + " in component "
                                                        + component.getClass().getName());
        }
        else if (methods.size() == 1)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Dynamic Entrypoint using method: " + component.getClass().getName() + "."
                             + ((Method)methods.get(0)).getName() + "(" + context.getClass().getName() + ")");
            }
            addMethod(component, (Method)methods.get(0), context.getClass());
            return invokeCurrent(component, context);
        }
        else
        {
            methods = ClassUtils.getSatisfiableMethods(component.getClass(),
                ClassUtils.getClassTypes(payload), true, true, ignoreMethods);
            if (methods.size() > 1)
            {
                throw new TooManySatisfiableMethodsException(component.getClass());
            }
            if (methods.size() == 1)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Dynamic Entrypoint using method: " + component.getClass().getName() + "."
                                 + ((Method)methods.get(0)).getName() + "(" + payload.getClass().getName()
                                 + ")");
                }
                addMethod(component, (Method)methods.get(0), payload.getClass());
                return invokeCurrent(component, payload);
            }
            else
            {
                throw new NoSatisfiableMethodsException(component.getClass(),
                    ClassUtils.getClassTypes(payload));
            }
        }
    }

    /**
     * This method can be used to validate that the method exists and is allowed to
     * be executed.
     * 
     * @param component
     * @param method
     * @param methodName
     * @throws Exception
     */
    protected void validateMethod(Object component, Method method, String methodName) throws Exception
    {
        boolean fallback = component instanceof Callable;
        if (method == null && !fallback)
        {
            throw new NoSuchMethodException(new Message(Messages.METHOD_X_WITH_PARAMS_X_NOT_FOUND_ON_X,
                methodName, "unknown", component.getClass().getName()).toString());
        }
        // This will throw NoSuchMethodException if it doesn't exist
        try
        {
            component.getClass().getMethod(method.getName(), method.getParameterTypes());
        }
        catch (Exception e)
        {
            if (!fallback) throw e;
        }
    }

    protected Method getMethod(Object component, Object arg)
    {
        return (Method)entryPoints.get(component.getClass().getName() + ":" + arg.getClass().getName());
    }

    protected void addMethod(Object component, Method method, Class arg)
    {
        entryPoints.put(component.getClass().getName() + ":" + arg.getName(), method);
        currentMethod = method;
    }

    /**
     * Will invoke the entry point method on the given component
     * 
     * @param component the component to invoke
     * @param arg the argument to pass to the method invocation
     * @return An object (if any) returned by the invocation
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private Object invokeCurrent(Object component, Object arg)
        throws InvocationTargetException, IllegalAccessException
    {
        String methodCall = null;
        if (logger.isDebugEnabled())
        {
            methodCall = component.getClass().getName() + "." + currentMethod.getName() + "("
                         + arg.getClass().getName() + ")";
            logger.debug("Invoking " + methodCall);
        }

        Object[] args;
        if (arg.getClass().isArray())
        {
            if (Object[].class.isAssignableFrom(arg.getClass()))
            {
                args = (Object[])arg;
            }
            else
            {
                args = new Object[]{arg};
            }
        }
        else if (arg instanceof NullPayload)
        {
            args = null;
        }
        else
        {
            args = new Object[]{arg};
        }
        Object result = currentMethod.invoke(component, args);
        if (logger.isDebugEnabled())
        {
            logger.debug("Result of call " + methodCall + " is " + (result == null ? "null" : "not null"));
        }
        return result;
    }

    public boolean isVoid()
    {
        if (currentMethod == null)
        {
            return false;
        }
        return currentMethod.getReturnType().getName().equals("void");
    }

    public String getMethodName()
    {
        if (currentMethod == null)
        {
            return null;
        }
        return currentMethod.getName();
    }
}
