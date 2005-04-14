/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.lifecycle.InitialisationException;

/**
 * <code>AlreadyInitialisedException</code> is thrown when a component or connector has
 * already been initialised
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class AlreadyInitialisedException extends InitialisationException
{
    /**
     * @param object the object that has been initialised can cannot be initialised again
     */
    public AlreadyInitialisedException(String name, Object object)
    {
        super(new Message(Messages.OBJECT_X_ALREADY_INITIALSIED, name), object);
    }
}
