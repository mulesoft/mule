/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.model.resolvers;

import org.mule.api.MuleEventContext;
import org.mule.api.model.InvocationResult;
import org.mule.routing.filters.WildcardFilter;
import org.mule.util.ClassUtils;
import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <code>ReflectEntryPointResolver</code> is used to determine the entry point on a service
 * after an event has been received for it. The entrypoint is  discovered using
 * the event payload type(s) as the argument using reflection. An entry point will try and match for
 * different argument types, so it's possible to have multiple entry points on a
 * single service.
 * <p/>
 * For multiple parameters the payload of context.getMessage().getPayload() should be an Array of objects.
 * If the message payload is of type {@link org.mule.transport.NullPayload} the resolver will look for a no-argument
 * method to call that doesn't match the set of ignoredMethods on the resolver.
 * <p/>
 * Also a set of 'ignored' methods are available (and the use can add others) to tell the resolver to not
 * resolve to these methods. The default ones are:
 * <ul>
 * <li>{@link #toString()}
 * <li>{@link #getClass()}
 * <li>{@link #notify}
 * <li>{@link #notifyAll}
 * <li>{@link #hashCode}
 * <li>{@link #wait}
 * <li>{@link java.lang.reflect.Proxy#getInvocationHandler}
 * <li>'is*'
 * <li>'get*'.
 * <li>'set*'.
 * </ul>
 * <p/> Note that wildcard expressions can be used.
 */
public class ReflectionEntryPointResolver extends AbstractEntryPointResolver
{
    // we don't want to match these methods when looking for a service method
    private Set<String> ignoredMethods = new HashSet<String>(Arrays.asList("equals",
            "getInvocationHandler", "set*", "toString",
            "getClass", "notify", "notifyAll", "wait", "hashCode", "clone", "is*", "get*"));

    protected WildcardFilter filter;

    public ReflectionEntryPointResolver()
    {
        updateFilter();
    }

    private void updateFilter()
    {
        filter = new WildcardFilter(StringUtils.join(ignoredMethods, ','));
    }

    /**
     * Returns an unmodifiable Set of ignoredMethods on this resolver
     * To add method to the resolver use {@link #addIgnoredMethod(String)}
     *
     * @return unmodifiable set of method names set on this resolver
     */
    public Set<String> getIgnoredMethods()
    {
        return Collections.unmodifiableSet(ignoredMethods);
    }

    public void setIgnoredMethods(Set<String> methods)
    {
        this.ignoredMethods = new HashSet<String>(methods);
        updateFilter();
    }

    public void addIgnoredMethod(String name)
    {
        this.ignoredMethods.add(name);
        updateFilter();
    }

    public boolean removeIgnoredMethod(String name)
    {
        boolean result = this.ignoredMethods.remove(name);
        updateFilter();
        return result;
    }

    /**
     * Will discover the entrypoint on the service using the payload type to figure out the method to call.
     * For multiple parameters the payload of context.getMessage().geTPayload() should be an Array of objects.
     * If the message payload is of type {@link org.mule.transport.NullPayload} the resolver will look for a no-argument
     * method to call that doesn't match the set of ignoredMethods on the resolver.
     *
     * @throws Exception
     */
    public InvocationResult invoke(Object component, MuleEventContext context) throws Exception
    {
        Object[] payload = getPayloadFromMessage(context);

        Method method;
        InvocationResult result;

        method = this.getMethodByArguments(component, payload);

        if (method != null)
        {
            return invokeMethod(component, method, payload);
        }

        Class<?>[] types = ClassUtils.getClassTypes(payload);

        // do any methods on the service accept a context?
        List<Method> methods = ClassUtils.getSatisfiableMethods(component.getClass(), types,
                isAcceptVoidMethods(), false, ignoredMethods, filter);

        int numMethods = methods.size();
        if (numMethods > 1)
        {
            result = new InvocationResult(this, InvocationResult.State.FAILED);
            // too many methods match the context argument
            result.setErrorTooManyMatchingMethods(component, types, StringMessageUtils.toString(methods));
            return result;

        }
        else if (numMethods == 1)
        {
            // found exact match for method with context argument
            method = this.addMethodByArguments(component, methods.get(0), payload);
        }
        else
        {
            methods = ClassUtils.getSatisfiableMethods(component.getClass(), 
                ClassUtils.getClassTypes(payload), true, true, ignoredMethods);

            numMethods = methods.size();

            if (numMethods > 1)
            {
                result = new InvocationResult(this, InvocationResult.State.FAILED);
                // too many methods match the context argument
                result.setErrorTooManyMatchingMethods(component, types, StringMessageUtils.toString(methods));
                return result;
            }
            else if (numMethods == 1)
            {
                // found exact match for payload argument
                method = this.addMethodByArguments(component, methods.get(0), payload);
            }
            else
            {
                result = new InvocationResult(this, InvocationResult.State.FAILED);
                // no method for payload argument either - bail out
                result.setErrorNoMatchingMethods(component, ClassUtils.getClassTypes(payload));
                return result;
            }
        }

        return invokeMethod(component, method, payload);
    }


    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ReflectionEntryPointResolver");
        sb.append("{ignoredMethods=").append(StringMessageUtils.toString(ignoredMethods));
        sb.append(", acceptVoidMethods=").append(isAcceptVoidMethods());
        sb.append('}');
        return sb.toString();
    }
}
