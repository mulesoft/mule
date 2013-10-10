/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.model.resolvers;

import org.mule.api.MuleException;
import org.mule.config.i18n.CoreMessages;

/**
 * <code>NoSatisfiableMethodsException</code> is thrown by EntryPointResolvers when
 * the service passed has no methods that meet the criteria of the configured
 * EntryPointResolver.
 *
 * @see org.mule.api.model.EntryPointResolver
 */
public class NoSatisfiableMethodsException extends MuleException
{
    /** Serial version */
    private static final long serialVersionUID = -4703387041767867189L;


    public NoSatisfiableMethodsException(Object component, String methodName)
    {
        super(CoreMessages.noEntryPointFoundForNoArgsMethod(component, methodName));
    }

    public NoSatisfiableMethodsException(Object component, Class<?>[] args)
    {
        super(CoreMessages.noEntryPointFoundWithArgs(component, args));
    }

    public NoSatisfiableMethodsException(Object component, Class<?> returnType)
    {
        super(CoreMessages.noMatchingMethodsOnObjectReturning(component, returnType));
    }
}
