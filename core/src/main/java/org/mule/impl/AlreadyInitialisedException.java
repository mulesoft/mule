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

import org.mule.config.i18n.CoreMessages;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;

/**
 * <code>AlreadyInitialisedException</code> is thrown when a component or connector
 * has already been initialised.
 */

public class AlreadyInitialisedException extends InitialisationException
{
    /** Serial version */
    private static final long serialVersionUID = 3121894155097428317L;

    /**
     * @param object the object that has been initialised and cannot be initialised
     *               again
     */
    public AlreadyInitialisedException(String name, Initialisable object)
    {
        super(CoreMessages.objectAlreadyInitialised(name), object);
    }
}
