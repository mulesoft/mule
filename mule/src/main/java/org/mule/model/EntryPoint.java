/* 
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * <code>EntryPoint</code> is a method on a Mule-managed component that is
 * invoked when an event for the component is received.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EntryPoint
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(EntryPoint.class);

    /**
     * the method on the object to invoke
     */
    private Method method;

    /**
     * Creates a new EntryPoint with the given method
     * 
     * @param method the method to invoke on the component
     */
    public EntryPoint(Method method)
    {
        this.method = method;
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
    public Object invoke(Object component, Object arg) throws InvocationTargetException, IllegalAccessException
    {
        String methodCall = null;
        if (logger.isDebugEnabled()) {
            methodCall = component.getClass().getName() + "." + method.getName() + "(" + arg.getClass().getName() + ")";
            logger.debug("Invoking " + methodCall);
        }

        Object result = method.invoke(component, new Object[] { arg });
        if (logger.isDebugEnabled()) {
            logger.debug("Result of call " + methodCall + " is " + result);
        }
        return result;
    }

    /**
     * Determines if the <code>EntryPoint</code> is avoid method or not
     * 
     * @return true if the method is void
     */
    public boolean isVoid()
    {
        return method.getReturnType().getName().equals("void");
    }

    /**
     * Gets the method name
     * 
     * @return the method name
     */
    public String getName()
    {
        if (method == null) {
            return null;
        }
        return method.getName();
    }

    /**
     * Gets the argument type for the method
     * 
     * @return the argument type. It should never be null
     */
    public Class getParameterType()
    {
        return method.getParameterTypes()[0];
    }

    /**
     * Gets the method return type of the method
     * 
     * @return the return type or null if the method is void
     */
    public Class getReturnType()
    {
        if (isVoid()) {
            return null;
        } else {
            return method.getReturnType();
        }
    }

    protected void setMethod(Method method)
    {
        this.method = method;
    }

    protected Method getMethod()
    {
        return method;
    }
}
