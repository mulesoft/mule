/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.config.i18n.CoreMessages;
import org.mule.umo.UMOException;

/**
 * <code>TooManySatisfiableMethodsException</code> is thrown by EntryPointResolvers
 * when the component passed has more than one method that meets the criteria of the
 * configured EntryPointResolver.
 *
 * @see org.mule.umo.model.UMOEntryPointResolver
 */
public class TooManySatisfiableMethodsException extends UMOException
{
    /** Serial version */
    private static final long serialVersionUID = 7856775581858822364L;

    public TooManySatisfiableMethodsException(Object component, Object[] types)
    {
        super(CoreMessages.tooManyAcceptableMethodsOnObjectForTypes(component, types));
    }

    public TooManySatisfiableMethodsException(Object component, Class returnType)
    {
        super(CoreMessages.tooManyMatchingMethodsOnObjectWhichReturn(component, returnType));
    }
}
