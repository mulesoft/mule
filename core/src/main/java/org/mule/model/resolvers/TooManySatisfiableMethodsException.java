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
 * <code>TooManySatisfiableMethodsException</code> is thrown by EntryPointResolvers
 * when the service passed has more than one method that meets the criteria of the
 * configured EntryPointResolver.
 *
 * @see org.mule.api.model.EntryPointResolver
 */
public class TooManySatisfiableMethodsException extends MuleException
{
    /** Serial version */
    private static final long serialVersionUID = 7856775581858822364L;

    public TooManySatisfiableMethodsException(Object component, Object[] types)
    {
        super(CoreMessages.tooManyAcceptableMethodsOnObjectForTypes(component, types));
    }

    public TooManySatisfiableMethodsException(Object component, Class<?> returnType)
    {
        super(CoreMessages.tooManyMatchingMethodsOnObjectWhichReturn(component, returnType));
    }
}
