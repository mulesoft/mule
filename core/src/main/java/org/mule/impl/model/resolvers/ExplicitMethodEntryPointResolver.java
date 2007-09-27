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

import org.mule.config.i18n.CoreMessages;
import org.mule.umo.UMOEventContext;
import org.mule.umo.model.InvocationResult;
import org.mule.util.ClassUtils;
import org.mule.util.StringMessageUtils;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An Entrypoint resolver that allows the user to set one or more acceptiple methd names to look for.
 * For each method reflection will be used to see if the method accepts the current payload types (
 * the results are cached to improve performance).
 * There has to be at least one method name set on this resolver
 */
public class ExplicitMethodEntryPointResolver extends AbstractEntryPointResolver
{
    private Set methods = new LinkedHashSet(2);

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

    public InvocationResult invoke(Object component, UMOEventContext context) throws Exception
    {
        if (methods == null || methods.size() == 0)
        {
            throw new IllegalStateException(CoreMessages.objectIsNull("methods").toString());
        }

        Object[] payload = getPayloadFromMessage(context);
        Class[] classTypes = ClassUtils.getClassTypes(payload);
        Method method = null;
        for (Iterator iterator = methods.iterator(); iterator.hasNext();)
        {
            String methodName = (String) iterator.next();
            method = getMethodByName(methodName, context);

            if (method == null)
            {
                method = ClassUtils.getMethod(component.getClass(), methodName, classTypes);
            }
            if (method != null)
            {
                addMethodByName(method, context);
                break;
            }
        }

        if (method == null)
        {
            InvocationResult result = new InvocationResult(InvocationResult.STATE_INVOKED_FAILED);
            result.setErrorNoMatchingMethods(component, classTypes, this);
            return result;
        }
        return invokeMethod(component, method, payload);
    }

    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append("ExplicitEntryPointResolver");
        sb.append("{methods=").append(StringMessageUtils.toString(methods));
        sb.append("{transformFirst=").append(isTransformFirst());
        sb.append(", acceptVoidMethods=").append(isAcceptVoidMethods());
        sb.append('}');
        return sb.toString();
    }

}
