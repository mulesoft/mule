/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.model;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.model.ModelException;
import org.mule.util.StringMessageUtils;

/**
 * <code>NoSatisfiableMethodsException</code> is thrown by EntryPointResolvers when
 * the component passed has no methods that meet the criteria of the configured
 * EntryPointResolver.
 * 
 * @see org.mule.umo.model.UMOEntryPointResolver
 */
public class NoSatisfiableMethodsException extends ModelException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -4703387041767867189L;

    /**
     * @param component
     */
    public NoSatisfiableMethodsException(Object component, Object args)
    {
        this(component, args, null);
    }

    public NoSatisfiableMethodsException(Object component, Object args, Exception cause)
    {
        super(new Message(Messages.NO_ENTRY_POINT_FOUND_ON_X_WITH_ARGS_X,
            StringMessageUtils.toString(component), StringMessageUtils.toString(args)), cause);
    }

}
