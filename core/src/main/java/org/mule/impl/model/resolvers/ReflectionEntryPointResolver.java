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
 */
public class ReflectionEntryPointResolver extends AbstractEntryPointResolver
{

    // we don't want to match these methods when looking for a service method
    private Set ignoredMethods = new HashSet(Arrays.asList(new String[]{"equals",
            "getInvocationHandler", "set*"}));

    protected WildcardFilter filter;


    private void updateFilter()
    {
        filter = new WildcardFilter(StringUtils.join(ignoredMethods, ','));
    }

    /**
     * Returns an unmodifable Set of ignorredMethods on this resolver
     * To add method to the resolver use {@link #addIgnorredMethod(String)}
     *
     * @return unmodifiable set of method names set on this resolver
     */
    public Set getIgnorredMethods()
    {
        return Collections.unmodifiableSet(ignoredMethods);
    }

    public void setIgnorredMethods(Set methods)
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

    public InvocationResult invoke(Object component, UMOEventContext context) throws Exception
    {
        Object[] payload = getPayloadFromMessage(context);

        Method method = null;
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
