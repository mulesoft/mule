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
 * <code>NoSatisfiableMethodsException</code> is thrown by EntryPointResolvers when
 * the component passed has no methods that meet the criteria of the configured
 * EntryPointResolver.
 * 
 * @see org.mule.umo.model.UMOEntryPointResolver
 */
public class NoSatisfiableMethodsException extends UMOException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -4703387041767867189L;

    /**
     * @param component
     */
    public NoSatisfiableMethodsException(Object component, List args)
    {
        this(component, args, null);
    }

    public NoSatisfiableMethodsException(Object component, List args, Exception cause)
    {
        super(new Message(Messages.NO_ENTRY_POINT_FOUND_ON_X_WITH_ARGS_X,
            StringMessageUtils.toString(component), StringMessageUtils.toString(args)), cause);
    }

    public NoSatisfiableMethodsException(Object component, Class[] args)
    {
        this(component, args, null);
    }

    public NoSatisfiableMethodsException(Object component, Class[] args, Exception cause)
    {
        super(new Message(Messages.NO_ENTRY_POINT_FOUND_ON_X_WITH_ARGS_X,
            StringMessageUtils.toString(component), StringMessageUtils.toString(args)), cause);
    }

    public NoSatisfiableMethodsException(Object component, Class returnType)
    {
        super(new Message(Messages.NO_MATCHING_METHODS_WHICH_RETURN_X_ON_X, StringMessageUtils.toString(component),
                returnType.getName()));
    }

    public NoSatisfiableMethodsException(Object component, Class returnType, Exception cause)
    {
        super(new Message(Messages.NO_MATCHING_METHODS_WHICH_RETURN_X_ON_X, StringMessageUtils.toString(component),
                returnType.getName(), cause));
    }

}
