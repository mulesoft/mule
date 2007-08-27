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

import org.mule.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleMessage;
import org.mule.impl.NoSatisfiableMethodsException;
import org.mule.impl.OptimizedRequestContext;
import org.mule.impl.TooManySatisfiableMethodsException;
import org.mule.impl.VoidResult;
import org.mule.providers.NullPayload;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.model.UMOEntryPoint;
import org.mule.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;
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
    protected static final Set IgnoredMethodNames = new HashSet(Arrays.asList(new String[]{"equals",
        "getInvocationHandler"}));

    // @GuardedBy(itself)
    private final ConcurrentMap entryPoints = new ConcurrentHashMap();

    public DynamicEntryPoint()
    {
        super();
    }

    protected Method addMethodByArgumentType(Method method, String payloadClass)
    {
        Method previousMethod = (Method) entryPoints.putIfAbsent(payloadClass, method);
        return (previousMethod != null ? previousMethod : method);
    }

    protected Method addMethodByName(Method method, String payloadClass)
    {
        String methodName = method.getName();

        ConcurrentMap argumentTypes = (ConcurrentMap) entryPoints.get(methodName);
        if (argumentTypes == null)
        {
            argumentTypes = new ConcurrentHashMap();
            ConcurrentMap previousTypes = (ConcurrentMap) entryPoints.putIfAbsent(methodName, argumentTypes);
            if (previousTypes != null)
            {
                argumentTypes = previousTypes;
            }
        }

        Method previousMethod = (Method) argumentTypes.putIfAbsent(payloadClass, method);
        return (previousMethod != null ? previousMethod : method);
    }

    protected Method getMethodByArgumentType(String argumentType)
    {
        return (Method) entryPoints.get(argumentType);
    }

    protected Method getMethodByName(String methodName, String argumentType)
    {
        ConcurrentMap argumentTypes = (ConcurrentMap) entryPoints.get(methodName);
        return (argumentTypes != null ? (Method) argumentTypes.get(argumentType) : null);
    }

    public Object invoke(Object component, UMOEventContext context) throws Exception
    {
        Method method = null;
        Object payload = null;

        // Transports such as SOAP need to ignore the method property
        boolean ignoreMethod = BooleanUtils.toBoolean((Boolean) context.getMessage().removeProperty(
            MuleProperties.MULE_IGNORE_METHOD_PROPERTY));

        if (!ignoreMethod)
        {
            // Check for method override and remove it from the event
            Object methodOverride = context.getMessage().removeProperty(MuleProperties.MULE_METHOD_PROPERTY);

            if (methodOverride instanceof Method)
            {
                // Methods are (hopefully) directly useable
                method = (Method) methodOverride;
            }
            else if (methodOverride != null)
            {
                payload = context.getTransformedMessage();
                String payloadClassName = payload.getClass().getName();

                // try lookup first
                String methodOverrideName = methodOverride.toString();
                method = this.getMethodByName(methodOverrideName, payloadClassName);

                // method is not yet in the cache, so find it by name
                if (method == null)
                {
                    // get all methods that match the current argument types
                    List matchingMethods = ClassUtils.getSatisfiableMethods(component.getClass(), ClassUtils
                        .getClassTypes(payload), true, true, IgnoredMethodNames);

                    // try to find the method matching the methodOverride
                    for (Iterator i = matchingMethods.iterator(); i.hasNext();)
                    {
                        Method candidate = (Method) i.next();
                        if (candidate.getName().equals(methodOverride))
                        {
                            method = candidate;
                            break;
                        }
                    }

                    // this will throw up unless the component is a Callable
                    this.validateMethod(component, method, methodOverrideName);

                    // if validateMethod didn't complain AND we have a valid method
                    // reference, add it to the cache
                    if (method != null)
                    {
                        method = this.addMethodByName(method, payloadClassName);
                    }
                }
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
                method = this.getMethodByArgumentType(context.getClass().getName());
                if (method == null)
                {
                    // if that failed we try to find the method by payload
                    payload = context.getTransformedMessage();
                    method = this.getMethodByArgumentType(payload.getClass().getName());
                    if (method != null)
                    {
                        OptimizedRequestContext.unsafeRewriteEvent(new MuleMessage(payload, context.getMessage()));
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
                .getClassTypes(context), true, false, IgnoredMethodNames);

            int numMethods = methods.size();
            if (numMethods > 1)
            {
                // too many methods match the context argument
                TooManySatisfiableMethodsException tmsmex = new TooManySatisfiableMethodsException(component
                    .getClass(), methods);
                throw new InvocationTargetException(tmsmex, "There must be only one method accepting "
                                + context.getClass().getName() + " in component "
                                + component.getClass().getName());
            }
            else if (numMethods == 1)
            {
                // found exact match for method with context argument
                payload = context;
                method = this.addMethodByArgumentType((Method) methods.get(0), payload.getClass().getName());
            }
            else
            {
                // no method for context: try payload
                payload = context.getTransformedMessage();
                OptimizedRequestContext.unsafeRewriteEvent(new MuleMessage(payload, context.getMessage()));

                methods = ClassUtils.getSatisfiableMethods(component.getClass(), ClassUtils
                    .getClassTypes(payload), true, true, IgnoredMethodNames);

                numMethods = methods.size();

                if (numMethods > 1)
                {
                    // too many methods match the payload argument
                    throw new TooManySatisfiableMethodsException(component.getClass(), methods);
                }
                else if (numMethods == 1)
                {
                    // found exact match for payload argument
                    method = this.addMethodByArgumentType((Method) methods.get(0), payload.getClass()
                        .getName());
                }
                else
                {
                    // no method for payload argument either - bail out
                    throw new NoSatisfiableMethodsException(component.getClass(), ClassUtils
                        .getClassTypes(payload));
                }
            }
        }

        if (payload == null)
        {
            payload = context.getTransformedMessage();
            OptimizedRequestContext.unsafeRewriteEvent(new MuleMessage(payload, context.getMessage()));
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Dynamic Entrypoint using method: " + component.getClass().getName() + "."
                            + method.getName() + "(" + payload.getClass().getName() + ")");
        }

        return this.invokeMethod(component, method, payload);
    }

    /**
     * This method will actually invoke the given method on the given component.
     */
    protected Object invokeMethod(Object component, Method method, Object argument)
        throws InvocationTargetException, IllegalAccessException
    {
        String methodCall = null;

        if (logger.isDebugEnabled())
        {
            methodCall = component.getClass().getName() + "." + method.getName() + "("
                            + argument.getClass().getName() + ")";
            logger.debug("Invoking " + methodCall);
        }

        // TODO MULE-1088: in order to properly support an array as argument for a
        // component method, the block below would need to be removed. Unfortunately
        // this would break the LoanBroker, which passes a BankQuoteRequest in an
        // array even though the correct method only takes a single non-Array
        // argument. This is most likely SOAP behaviour; see the JIRA for more.
        // It is not entirely clearErrors to me whether this is a bug in the LoanBroker or
        // intended behaviour, and what the check for Object[] assignment
        // compatibility is supposed to do/prevent in the first place?
        // Any kind of interpretation/rewriting of the array argument is pretty
        // much futile at this point, because we have already found a matching method
        // - otherwise we wouldn't be here!

        // this will wrap the given argument for the invocation
        Object[] invocationArgs;

        if (argument.getClass().isArray())
        {
            if (Object[].class.isAssignableFrom(argument.getClass()))
            {
                invocationArgs = (Object[]) argument;
            }
            else
            {
                invocationArgs = new Object[]{argument};
            }
        }
        else if (argument instanceof NullPayload)
        {
            invocationArgs = null;
        }
        else
        {
            invocationArgs = new Object[]{argument};
        }

        Object result = method.invoke(component, invocationArgs);
        if (method.getReturnType().equals(Void.TYPE))
        {
            result = VoidResult.getInstance();
        }

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
    protected void validateMethod(Object component, Method method, String methodName)
        throws NoSuchMethodException
    {
        boolean fallback = component instanceof Callable;

        if (method != null)
        {
            // This will throw NoSuchMethodException if it doesn't exist
            try
            {
                component.getClass().getMethod(method.getName(), method.getParameterTypes());
            }
            catch (NoSuchMethodException e)
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
                throw new NoSuchMethodException(
                    CoreMessages.methodWithParamsNotFoundOnObject(methodName, "unknown", 
                        component.getClass()).toString());
            }
        }
    }

}
