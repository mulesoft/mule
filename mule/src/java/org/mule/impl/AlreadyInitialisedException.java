/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl;

import org.mule.InitialisationException;

/**
 * <code>AlreadyInitialisedException</code> is thrown when a component or connector has
 * already been initialised
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class AlreadyInitialisedException extends InitialisationException
{
    /**
     * @param message
     */
    public AlreadyInitialisedException(String message)
    {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public AlreadyInitialisedException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
