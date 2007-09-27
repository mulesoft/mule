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
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.model.InvocationResult;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;

import org.apache.commons.lang.BooleanUtils;

/**
 * This resolver will look for a 'method' property on the incoming event to determine which method to invoke
 * Users can customise the name of the property used to look up the method name on the event
 */
public class MethodHeaderPropertyEntryPointResolver extends AbstractEntryPointResolver
{

    private String methodProperty = MuleProperties.MULE_METHOD_PROPERTY;

    public String getMethodProperty()
    {
        return methodProperty;
    }

    public void setMethodProperty(String methodProperty)
    {
        this.methodProperty = methodProperty;
    }

    public InvocationResult invoke(Object component, UMOEventContext context) throws Exception
    {
        //TODO: RM* This is a hack that can be fixed by introducing property scoping on the message
        // Transports such as SOAP need to ignore the method property
        boolean ignoreMethod = BooleanUtils.toBoolean((Boolean) context.getMessage().removeProperty(
                MuleProperties.MULE_IGNORE_METHOD_PROPERTY));

        if (ignoreMethod)
        {
            //TODO: Removed once we have property scoping
            InvocationResult result = new InvocationResult(InvocationResult.STATE_INVOKE_NOT_SUPPORTED);
            result.setErrorMessage("Property: " + MuleProperties.MULE_IGNORE_METHOD_PROPERTY + " was set so skipping this resolver: " + this);
            return result;
        }

        //TODO: with scoped properties we wouldn't need to remove the property here
        Object methodProp = context.getMessage().removeProperty(getMethodProperty());
        if (methodProp == null)
        {
            InvocationResult result = new InvocationResult(InvocationResult.STATE_INVOKED_FAILED);
            // no method for the explicit method header
            result.setErrorMessage(CoreMessages.propertyIsNotSetOnEvent(getMethodProperty()).toString());
            return result;
        }

        Method method;
        String methodName;
        if (methodProp instanceof Method)
        {
            method = (Method) methodProp;
            methodName = method.getName();
        }
        else
        {
            methodName = methodProp.toString();
            method = getMethodByName(methodName, context);
        }

        if (method != null && method.getParameterTypes().length == 0)
        {
            return invokeMethod(component, method, ClassUtils.NO_ARGS_TYPE);
        }

        Object[] payload = getPayloadFromMessage(context);

        if (method == null)
        {
            Class[] classTypes = ClassUtils.getClassTypes(payload);
            try
            {
                method = component.getClass().getMethod(methodName, classTypes);
            }
            catch (NoSuchMethodException e)
            {
                InvocationResult result = new InvocationResult(InvocationResult.STATE_INVOKED_FAILED);
                result.setErrorNoMatchingMethods(component, classTypes, this);
                return result;

            }
        }

        validateMethod(component, method);
        addMethodByName(method, context);

        return invokeMethod(component, method, payload);

    }

    /**
     * This method can be used to validate that the method exists and is allowed to
     * be executed.
     */
    protected void validateMethod(Object component, Method method)
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
                        CoreMessages.methodWithParamsNotFoundOnObject(method.getName(), "unknown",
                                component.getClass()).toString());
            }
        }
    }

    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append("MethodHeaderPropertyEntryPointResolver");
        sb.append("{methodHeader=").append(methodProperty);
        sb.append("transformFirst=").append(isTransformFirst());
        sb.append(", acceptVoidMethods=").append(isAcceptVoidMethods());
        sb.append('}');
        return sb.toString();
    }

}
