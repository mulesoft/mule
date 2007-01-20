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

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DynamicEntryPoint</code> is used to detemine the entry point on a bean
 * after an event has been received for it. The entrypoint is then discovered using
 * the event payload type as the argument. An entry point will try and match for
 * different argument types, so it's possible to have multiple entry points on a
 * single component.
 */

public class DynamicEntryPoint implements UMOEntryPoint
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(DynamicEntryPoint.class);

    // we don't want to match these methods when looking for a service method
    protected static final Set ignoredMethods = new HashSet(Arrays.asList(new String[]{"equals",
        "getInvocationHandler"}));

    // @GuardedBy(itself)
    private final ConcurrentMap entryPoints = new ConcurrentHashMap();

    private volatile Method currentMethod;

    public DynamicEntryPoint()
    {
        super();
    }

    public boolean isVoid()
    {
        return (currentMethod != null ? currentMethod.getReturnType().getName().equals("void") : false);
    }

    public String getMethodName()
    {
        return (currentMethod != null ? currentMethod.getName() : null);
    }

    public Class[] getParameterTypes()
    {
        return (currentMethod != null ? currentMethod.getParameterTypes() : null);
    }

    public Object invoke(Object component, UMOEventContext context) throws Exception
    {
        Method method = null;
        Object payload = null;

        // Transports such as SOAP need to ignore the method property
        boolean ignoreMethod = BooleanUtils.toBoolean((Boolean)context.getMessage().removeProperty(
            MuleProperties.MULE_IGNORE_METHOD_PROPERTY));

        if (!ignoreMethod)
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

        // do we need to lookup the method?
        if (method == null)
        {
            // prefer Callable
            if (component instanceof Callable)
            {
                method = Callable.class.getMethods()[0];
                payload = context;
            }
            else
            {
                // no Callable: try to find the method dynamically
                // first we try to find a method that accepts UMOEventContext
                method = (Method)entryPoints.get(context.getClass().getName());
                if (method == null)
                {
                    // if that failed we try to find the method by payload
                    payload = context.getTransformedMessage();
                    method = (Method)entryPoints.get(payload.getClass().getName());
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

        // method is not in cache, so find it
        if (method == null)
        {
            // do any methods on the component accept a context?
            List methods = ClassUtils.getSatisfiableMethods(component.getClass(), ClassUtils
                .getClassTypes(context), true, false, ignoredMethods);

            int numMethods = methods.size();
            if (numMethods > 1)
            {
                // too many methods match the context argument
                TooManySatisfiableMethodsException tmsmex = new TooManySatisfiableMethodsException(component
                    .getClass());
                throw new InvocationTargetException(tmsmex, "There must be only one method accepting "
                                + context.getClass().getName() + " in component "
                                + component.getClass().getName());
            }
            else if (numMethods == 1)
            {
                // found exact match for method with context argument
                if (logger.isDebugEnabled())
                {
                    logger.debug("Dynamic Entrypoint using method: " + component.getClass().getName() + "."
                                    + ((Method)methods.get(0)).getName() + "(" + context.getClass().getName()
                                    + ")");
                }

                method = (Method)methods.get(0);
                Method previous = (Method)entryPoints.putIfAbsent(context.getClass().getName(), method);
                if (previous != null)
                {
                    method = previous;
                }

                payload = context;
            }
            else
            {
                // no method for context: try payload
                payload = context.getTransformedMessage();
                RequestContext.rewriteEvent(new MuleMessage(payload, context.getMessage()));

                methods = ClassUtils.getSatisfiableMethods(component.getClass(), ClassUtils
                    .getClassTypes(payload), true, true, ignoredMethods);

                numMethods = methods.size();

                if (numMethods > 1)
                {
                    // too many methods match the payload argument
                    throw new TooManySatisfiableMethodsException(component.getClass());
                }
                else if (numMethods == 1)
                {
                    // found exact match for payload argument
                    method = (Method)methods.get(0);
                    Method previous = (Method)entryPoints.putIfAbsent(payload.getClass().getName(), method);
                    if (previous != null)
                    {
                        method = previous;
                    }

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Dynamic Entrypoint using method: " + component.getClass().getName()
                                        + "." + method.getName() + "(" + payload.getClass().getName() + ")");
                    }
                }
                else
                {
                    // no method for payload argument either - bail out
                    throw new NoSatisfiableMethodsException(component.getClass(), ClassUtils
                        .getClassTypes(payload));
                }
            }
        }

        // remember the last invoked method
        currentMethod = method;

        if (payload == null)
        {
            payload = context.getTransformedMessage();
            RequestContext.rewriteEvent(new MuleMessage(payload, context.getMessage()));
        }

        return this.invokeMethod(component, method, payload);
    }

    /**
     * This method will actually invoke the given method on the given component.
     */
    protected Object invokeMethod(Object component, Method method, Object arg)
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

        Object result = method.invoke(component, args);

        if (logger.isDebugEnabled())
        {
            logger.debug("Result of call " + methodCall + " is " + (result == null ? "null" : "not null"));
        }

        return result;
    }

    /**
     * This method can be used to validate that the method exists and is allowed to
     * be executed.
     */
    protected void validateMethod(Object component, Method method, String methodName) throws Exception
    {
        boolean fallback = component instanceof Callable;

        if (method != null)
        {
            // This will throw NoSuchMethodException if it doesn't exist
            try
            {
                component.getClass().getMethod(method.getName(), method.getParameterTypes());
            }
            catch (Exception e)
            {
                if (!fallback)
                {
                    throw e;
                }
            }
        }
        else
        {
            if (!fallback)
            {
                throw new NoSuchMethodException(new Message(Messages.METHOD_X_WITH_PARAMS_X_NOT_FOUND_ON_X,
                    methodName, "unknown", component.getClass().getName()).toString());
            }
        }
    }

}
