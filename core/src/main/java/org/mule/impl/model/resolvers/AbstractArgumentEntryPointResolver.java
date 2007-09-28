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

import org.mule.umo.UMOEventContext;
import org.mule.umo.model.InvocationResult;
import org.mule.util.ClassUtils;
import org.mule.util.StringMessageUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A base class that allows implementing resolvers to define what parameters it is expecting.  Currently
 * there are two implementations of this {@link org.mule.impl.model.resolvers.NoArgumentsEntryPointResolver}, that
 * allows meothds with no arguments to be invoked and {@link org.mule.impl.model.resolvers.ArrayEntryPointResolver} that
 * allows for methods that accept an array type to be invoked.
 * <p/>
 * Users can set explicit method names on this resolver to control which methods are allowed to be called. Also a set of
 * 'ingorred' methods are available (and the use can add others) to tell the resolver to not resolve to these methods.
 * The default ones are:
 * <ul>
 * <li>{@link #toString()}
 * <li>{@link #getClass()}
 * <li>{@link #notify}
 * <li>{@link #notifyAll}
 * <li>{@link #hashCode}
 * <li>{@link #wait}
 * <li>{@link Cloneable#clone()}
 * <li>'is*'
 * <li>'get*'.
 * </ul>
 * <p/> Note that wildcard expressions can be used.
 */
public abstract class AbstractArgumentEntryPointResolver extends ReflectionEntryPointResolver
{
    private Set methods = new HashSet(2);

    private boolean enableDiscovery = true;

    public AbstractArgumentEntryPointResolver()
    {
        //By default No arg methods without a return type should be supported
        setAcceptVoidMethods(true);
        // we don't want to match these methods when looking for a service method
        //If you add to this list please change the javaDoc above too.
        setIgnoredMethods(new HashSet(Arrays.asList(new String[]{"toString",
                "getClass", "notify", "notifyAll", "wait", "hashCode", "clone", "is*", "get*"})));
    }

    public Set getMethods()
    {
        return methods;
    }

    public void setMethods(Set methods)
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

    public InvocationResult invoke(Object component, UMOEventContext context) throws Exception
    {
        Method method = null;
        Object[] payload = getPayloadFromMessage(context);

        if (payload == null)
        {
            return new InvocationResult(InvocationResult.STATE_INVOKE_NOT_SUPPORTED);
        }

        for (Iterator iterator = methods.iterator(); iterator.hasNext();)
        {
            String methodName = (String) iterator.next();
            method = getMethodByName(methodName, context);

            if (method == null)
            {
                method = ClassUtils.getMethod(component.getClass(), methodName, getMethodArgumentTypes(payload));
            }
            if (method != null)
            {
                addMethodByName(method, context);
                break;
            }
        }
        //If the method wasn't explicitly set, lets try and discover it
        if (method == null)
        {
            if (isEnableDiscovery())
            {
                Class[] argTypes = getMethodArgumentTypes(payload);
                List methods = ClassUtils.getSatisfiableMethods(component.getClass(), argTypes,
                        isAcceptVoidMethods(), false, getIgnoredMethods(), filter);

                if (methods.size() > 1)
                {
                    InvocationResult result = new InvocationResult(InvocationResult.STATE_INVOKED_FAILED);
                    // too many methods match the payload argument
                    result.setErrorTooManyMatchingMethods(component, argTypes, this);
                    return result;
                }
                else if (methods.size() == 1)
                {
                    // found exact match for payload argument
                    method = this.addMethodByArguments(component, (Method) methods.get(0), getPayloadFromMessage(context));
                }
                else
                {
                    InvocationResult result = new InvocationResult(InvocationResult.STATE_INVOKED_FAILED);
                    // no method for payload argument either - bail out
                    result.setErrorNoMatchingMethods(component, ClassUtils.NO_ARGS_TYPE, this);
                    return result;
                }
            }
            else
            {
                InvocationResult result = new InvocationResult(InvocationResult.STATE_INVOKED_FAILED);
                // no method for the explicit methods either
                result.setErrorNoMatchingMethodsCalled(component, StringMessageUtils.toString(methods), this);
                return result;
            }
        }
        return invokeMethod(component, method, getPayloadFromMessage(context));
    }

    protected abstract Class[] getMethodArgumentTypes(Object[] payload);

    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append(ClassUtils.getClassName(getClass()));
        sb.append("{methods=").append(StringMessageUtils.toString(methods));
        sb.append("{transformFirst=").append(isTransformFirst());
        sb.append(", acceptVoidMethods=").append(isAcceptVoidMethods());
        sb.append('}');
        return sb.toString();
    }
}
