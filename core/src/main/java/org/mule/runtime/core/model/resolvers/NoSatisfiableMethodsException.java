/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
