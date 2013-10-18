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
