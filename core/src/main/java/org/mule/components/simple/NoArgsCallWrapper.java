/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.simple;

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.NoSatisfiableMethodsException;
import org.mule.impl.VoidResult;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import java.lang.reflect.Method;

public class NoArgsCallWrapper implements Callable, Initialisable
{
    /**
     * To allow injecting the delegate instead of instanciating it.
     */
    private Object delegateInstance;
    private String delegateMethod;
    private String delegateClass;

    /**
     * Method used to perform any initialisation work. If a fatal error occurs during initialisation an
     * <code>InitialisationException</code> should be thrown, causing the Mule instance to shutdown. If the error is
     * recoverable, say by retrying to connect, a <code>RecoverableException</code> should be thrown. There is no
     * guarantee that by throwing a Recoverable exception that the Mule instance will not shut down.
     *
     * @throws org.mule.umo.lifecycle.InitialisationException
     *          if a fatal error occurs causing the Mule instance to shutdown
     * @throws org.mule.umo.lifecycle.RecoverableException
     *          if an error occurs that can be recovered from
     */
    public void initialise() throws InitialisationException
    {
        if (delegateInstance == null)
        {
            // delegateInstance null -> both class and method required
            if (StringUtils.isBlank(delegateClass) || StringUtils.isBlank(delegateMethod))
            {
                throw new InitialisationException(CoreMessages.noDelegateClassAndMethodProvidedForNoArgsWrapper(), this);
            }
        }
        else
        {
            // delegateInstance provided -> no delegate class configured
            if (StringUtils.isNotBlank(delegateClass))
            {
                throw new InitialisationException(CoreMessages.noDelegateClassIfDelegateInstanceSpecified(), this);
            }
        }

        if (StringUtils.isBlank(delegateMethod))
        {
            throw new InitialisationException(CoreMessages.objectIsNull("delegateMethod"), this);
        }
    }

    public Object onCall(UMOEventContext context) throws Exception
    {
        Class clazz = delegateInstance == null ? ClassUtils.loadClass(delegateClass, getClass())
                                               : delegateInstance.getClass();

        Method method = ClassUtils.getMethod(clazz, delegateMethod, null);
        if (method == null)
        {
            throw new NoSatisfiableMethodsException(clazz, delegateMethod);
        }
        if (delegateInstance == null)
        {
            delegateInstance = clazz.newInstance();
        }
        Object result = method.invoke(delegateInstance, null);
        if (Void.TYPE.equals(method.getReturnType()))
        {
            result = VoidResult.getInstance();
        }
        return result;
    }

    /**
     * Getter for property 'delegateInstance'.
     *
     * @return Value for property 'delegateInstance'.
     */
    public Object getDelegateInstance()
    {
        return delegateInstance;
    }

    /**
     * Setter for property 'delegateInstance'.
     *
     * @param delegateInstance Value to set for property 'delegateInstance'.
     */
    public void setDelegateInstance(final Object delegateInstance)
    {
        this.delegateInstance = delegateInstance;
    }

    /**
     * Getter for property 'delegateMethod'.
     *
     * @return Value for property 'delegateMethod'.
     */
    public String getDelegateMethod()
    {
        return delegateMethod;
    }

    /**
     * Setter for property 'delegateMethod'.
     *
     * @param delegateMethod Value to set for property 'delegateMethod'.
     */
    public void setDelegateMethod(final String delegateMethod)
    {
        this.delegateMethod = delegateMethod;
    }

    /**
     * Getter for property 'delegateClass'.
     *
     * @return Value for property 'delegateClass'.
     */
    public String getDelegateClass()
    {
        return delegateClass;
    }

    /**
     * Setter for property 'delegateClass'.
     *
     * @param delegateClass Value to set for property 'delegateClass'.
     */
    public void setDelegateClass(final String delegateClass)
    {
        this.delegateClass = delegateClass;
    }
}
