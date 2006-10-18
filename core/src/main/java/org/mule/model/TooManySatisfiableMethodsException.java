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
 * <code>TooManySatisfiableMethodsException</code> is thrown by EntryPointResolvers
 * when the component passed has more than one method that meets the criteria of the
 * configured EntryPointResolver.
 * 
 * @see org.mule.umo.model.UMOEntryPointResolver
 */
public class TooManySatisfiableMethodsException extends ModelException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 7856775581858822364L;

    /**
     * @param component
     */
    public TooManySatisfiableMethodsException(Object component)
    {
        super(new Message(Messages.TOO_MANY_ENTRY_POINTS_ON_X, StringMessageUtils.toString(component)));
    }

    public TooManySatisfiableMethodsException(Object component, Exception cause)
    {
        super(new Message(Messages.TOO_MANY_ENTRY_POINTS_ON_X, StringMessageUtils.toString(component)), cause);
    }

}
