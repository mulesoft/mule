/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.model.resolvers;

import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.UMOEventContext;
import org.mule.umo.model.InvocationResult;
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
 * <code>ReflectEntryPointResolver</code> is used to detemine the entry point on a component
 * after an event has been received for it. The entrypoint is  discovered using
 * the event payload type(s) as the argument using reflection. An entry point will try and match for
 * different argument types, so it's possible to have multiple entry points on a
 * single component.
 * <p/>
 * For multiple parameters the payload of context.getMessage().geTPayload() should be an Array of objects.
 * If the message payload is of type {@link org.mule.providers.NullPayload} the resolver will look for a no-argument
 * method to call that doesn't match the set of ignoredMethods on the resover.
 * <p/>
 * Also a set of 'ingorred' methods are available (and the use can add others) to tell the resolver to not
 * resolve to these methods. The default ones are:
 * <ul>
 * <li>{@link #toString()}
 * <li>{@link #getClass()}
 * <li>{@link #notify}
 * <li>{@link #notifyAll}
 * <li>{@link #hashCode}
 * <li>{@link #wait}
 * <li>{@link java.lang.reflect.Proxy#getInvocationHandler}
 * <li>{@link Cloneable#clone()}
 * <li>'is*'
 * <li>'get*'.
 * <li>'set*'.
 * </ul>
 * <p/> Note that wildcard expressions can be used.
 */
public class ReflectionEntryPointResolver extends AbstractEntryPointResolver
{

    // we don't want to match these methods when looking for a service method
    private Set ignoredMethods = new HashSet(Arrays.asList(new String[]{"equals",
            "getInvocationHandler", "set*", "toString",
            "getClass", "notify", "notifyAll", "wait", "hashCode", "clone", "is*", "get*"}));

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
     * Returns an unmodifable Set of ignoredMethods on this resolver
     * To add method to the resolver use {@link #addIgnorredMethod(String)}
     *
     * @return unmodifiable set of method names set on this resolver
     */
    public Set getIgnoredMethods()
    {
        return Collections.unmodifiableSet(ignoredMethods);
    }

    public void setIgnoredMethods(Set methods)
    {
        this.ignoredMethods = methods;
        updateFilter();
    }

    public void addIgnorredMethod(String name)
    {
        this.ignoredMethods.add(name);
        updateFilter();
    }

    public boolean removeIgnorredMethod(String name)
    {
        boolean result = this.ignoredMethods.remove(name);
        updateFilter();
        return result;
    }

    /**
     * Will discover the entrypoint on the component using the payload type to figure out the method to call.
     * For multiple parameters the payload of context.getMessage().geTPayload() should be an Array of objects.
     * If the message payload is of type {@link org.mule.providers.NullPayload} the resolver will look for a no-argument
     * method to call that doesn't match the set of ignoredMethods on the resover.
     *
     * @param component
     * @param context
     * @return
     * @throws Exception
     */
    public InvocationResult invoke(Object component, UMOEventContext context) throws Exception
    {
        Object[] payload = getPayloadFromMessage(context);

        Method method;
        InvocationResult result;

        method = this.getMethodByArguments(component, payload);

        if (method != null)
        {
            return invokeMethod(component, method, payload);
        }

        Class[] types = ClassUtils.getClassTypes(payload);

        // do any methods on the component accept a context?
        List methods = ClassUtils.getSatisfiableMethods(component.getClass(), types,
                isAcceptVoidMethods(), false, ignoredMethods, filter);

        int numMethods = methods.size();
        if (numMethods > 1)
        {
            result = new InvocationResult(InvocationResult.STATE_INVOKED_FAILED);
            // too many methods match the context argument
            result.setErrorTooManyMatchingMethods(component, types, StringMessageUtils.toString(methods), this);
            return result;

        }
        else if (numMethods == 1)
        {
            // found exact match for method with context argument
            method = this.addMethodByArguments(component, (Method) methods.get(0), payload);
        }
        else
        {
            methods = ClassUtils.getSatisfiableMethods(component.getClass(), ClassUtils
                    .getClassTypes(payload), true, true, ignoredMethods);

            numMethods = methods.size();

            if (numMethods > 1)
            {
                result = new InvocationResult(InvocationResult.STATE_INVOKED_FAILED);
                // too many methods match the context argument
                result.setErrorTooManyMatchingMethods(component, types, StringMessageUtils.toString(methods), this);
                return result;
            }
            else if (numMethods == 1)
            {
                // found exact match for payload argument
                method = this.addMethodByArguments(component, (Method) methods.get(0), payload);
            }
            else
            {
                result = new InvocationResult(InvocationResult.STATE_INVOKED_FAILED);
                // no method for payload argument either - bail out
                result.setErrorNoMatchingMethods(component, ClassUtils.getClassTypes(payload), this);
                return result;
            }
        }

        return invokeMethod(component, method, payload);
    }


    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append("ReflectionEntryPointResolver");
        sb.append("{ignoredMethods=").append(StringMessageUtils.toString(ignoredMethods));
        sb.append("{transformFirst=").append(isTransformFirst());
        sb.append(", acceptVoidMethods=").append(isAcceptVoidMethods());
        sb.append('}');
        return sb.toString();
    }
}
