/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOException;
import org.mule.util.StringMessageUtils;

import java.util.List;

/**
 * <code>TooManySatisfiableMethodsException</code> is thrown by EntryPointResolvers
 * when the component passed has more than one method that meets the criteria of the
 * configured EntryPointResolver.
 * 
 * @see org.mule.umo.model.UMOEntryPointResolver
 */
public class TooManySatisfiableMethodsException extends UMOException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 7856775581858822364L;

    /**
     * @param component
     */
    public TooManySatisfiableMethodsException(Object component, List types)
    {
        super(new Message(Messages.TOO_MANY_ACCEPTIBLE_METHODS_ON_X_FOR_TYPES_X, StringMessageUtils.toString(component),
                StringMessageUtils.toString(types)));
    }

    public TooManySatisfiableMethodsException(Object component, List types, Exception cause)
    {
        super(new Message(Messages.TOO_MANY_ACCEPTIBLE_METHODS_ON_X_FOR_TYPES_X, StringMessageUtils.toString(component),
                StringMessageUtils.toString(types)), cause);
    }

    public TooManySatisfiableMethodsException(Object component, Object[] types)
    {
        super(new Message(Messages.TOO_MANY_ACCEPTIBLE_METHODS_ON_X_FOR_TYPES_X, StringMessageUtils.toString(component),
                StringMessageUtils.toString(types)));
    }

    public TooManySatisfiableMethodsException(Object component, Object[] types, Exception cause)
    {
        super(new Message(Messages.TOO_MANY_ACCEPTIBLE_METHODS_ON_X_FOR_TYPES_X, StringMessageUtils.toString(component),
                StringMessageUtils.toString(types)), cause);
    }

    public TooManySatisfiableMethodsException(Object component, Class returnType)
    {
        super(new Message(Messages.TOO_MANY_MATCHING_METHODS_WHICH_RETURN_X_ON_X, StringMessageUtils.toString(component),
                returnType.getName()));
    }

    public TooManySatisfiableMethodsException(Object component, Class returnType, Exception cause)
    {
        super(new Message(Messages.TOO_MANY_MATCHING_METHODS_WHICH_RETURN_X_ON_X, StringMessageUtils.toString(component),
                returnType.getName(), cause));
    }

}
