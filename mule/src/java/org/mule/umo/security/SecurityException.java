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
package org.mule.umo.security;

import org.mule.config.i18n.Message;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOMessage;

/**
 * <code>SecurityException</code> is a generic security exception
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public abstract class SecurityException extends MessagingException
{
    protected SecurityException(Message message, UMOMessage umoMessage)
    {
        super(message, umoMessage);
    }

    protected SecurityException(Message message, UMOMessage umoMessage, Throwable cause)
    {
        super(message, umoMessage, cause);
    }
}
