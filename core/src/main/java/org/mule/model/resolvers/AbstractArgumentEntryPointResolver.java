/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.model.resolvers;

import org.mule.api.MuleEventContext;
import org.mule.api.model.InvocationResult;
import org.mule.util.ClassUtils;
import org.mule.util.StringMessageUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A base class that allows implementing resolvers to define what parameters it is expecting.  Currently
 * there are two implementations of this {@link org.mule.model.resolvers.NoArgumentsEntryPointResolver}, that
 * allows methods with no arguments to be invoked and {@link org.mule.model.resolvers.ArrayEntryPointResolver} that
 * allows for methods that accept an array type to be invoked.
 * <p/>
 * Users can set explicit method names on this resolver to control which methods are allowed to be called. Also a set of
 * 'ignored' methods are available (and the use can add others) to tell the resolver to not resolve to these methods.
 * The default ones are:
 * <ul>
 * <li>{@link #toString()}
 * <li>{@link #getClass()}
 * <li>{@link #notify}
 * <li>{@link #notifyAll}
 * <li>{@link #hashCode}
 * <li>{@link #wait}
 * <li>'is*'
 * <li>'get*'.
 * </ul>
 * <p/> Note that wildcard expressions can be used.
 */
public abstract class AbstractArgumentEntryPointResolver extends ReflectionEntryPointResolver
{
    private Set<String> methods = new HashSet<String>(2);

    private boolean enableDiscovery = true;

    public AbstractArgumentEntryPointResolver()
    {
        //By default No arg methods without a return type should be supported
        setAcceptVoidMethods(true);
        // we don't want to match these methods when looking for a service method
        //If you add to this list please change the javaDoc above too.
        setIgnoredMethods(new HashSet<String>(Arrays.asList("toString",
                "getClass", "notify", "notifyAll", "wait", "hashCode", "clone", "is*", "get*")));
    }

    public Set<String> getMethods()
    {
        return methods;
    }

    public void setMethods(Set<String> methods)
    {
        this.methods = methods;
    }

    public void addMethod(String name)
    {
        this.methods.add(name);
    }

    public boolean removeMethod(String name)
    {
        return this.methods.remove(name);
    }


    public boolean isEnableDiscovery()
    {
        return enableDiscovery;
    }

    public void setEnableDiscovery(boolean enableDiscovery)
    {
        this.enableDiscovery = enableDiscovery;
    }

    @Override
    public InvocationResult invoke(Object component, MuleEventContext context) throws Exception
    {
        Method method = null;
        Object[] payload = getPayloadFromMessage(context);

        if (payload == null)
        {
            return new InvocationResult(this, InvocationResult.State.NOT_SUPPORTED);
        }

        for (String methodName : methods)
        {
            method = getMethodByName(component, methodName, context);

            if (method == null)
            {
                method = ClassUtils.getMethod(component.getClass(), methodName, 
                    getMethodArgumentTypes(payload));
            }
            if (method != null)
            {
                addMethodByName(component, method, context);
                break;
            }
        }
        //If the method wasn't explicitly set, lets try and discover it
        if (method == null)
        {
            if (isEnableDiscovery())
            {
                Class<?>[] argTypes = getMethodArgumentTypes(payload);
                List<Method> methods = ClassUtils.getSatisfiableMethods(component.getClass(), argTypes,
                        isAcceptVoidMethods(), false, getIgnoredMethods(), filter);

                if (methods.size() > 1)
                {
                    InvocationResult result = new InvocationResult(this, InvocationResult.State.FAILED);
                    // too many methods match the payload argument
                    result.setErrorTooManyMatchingMethods(component, argTypes, 
                        StringMessageUtils.toString(methods));
                    return result;
                }
                else if (methods.size() == 1)
                {
                    // found exact match for payload argument
                    method = this.addMethodByArguments(component, methods.get(0), 
                        getPayloadFromMessage(context));
                }
                else
                {
                    InvocationResult result = new InvocationResult(this, InvocationResult.State.FAILED);
                    // no method for payload argument either - bail out
                    result.setErrorNoMatchingMethods(component, ClassUtils.NO_ARGS_TYPE);
                    return result;
                }
            }
            else
            {
                InvocationResult result = new InvocationResult(this, InvocationResult.State.FAILED);
                // no method for the explicit methods either
                result.setErrorNoMatchingMethodsCalled(component, StringMessageUtils.toString(methods));
                return result;
            }
        }
        return invokeMethod(component, method, getPayloadFromMessage(context));
    }

    protected abstract Class<?>[] getMethodArgumentTypes(Object[] payload);

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(ClassUtils.getClassName(getClass()));
        sb.append("{methods=").append(StringMessageUtils.toString(methods));
        sb.append(", acceptVoidMethods=").append(isAcceptVoidMethods());
        sb.append('}');
        return sb.toString();
    }
}
