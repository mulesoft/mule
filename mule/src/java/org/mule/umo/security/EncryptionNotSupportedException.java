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
package org.mule.umo.security;

import org.mule.config.i18n.Message;
import org.mule.umo.UMOMessage;

/**
 * <code>EncryptionNotSupportedException</code> is thrown if an algorithm is
 * set in the MULE_USER header but it doesn't match the algorithm set on
 * the security filter
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class EncryptionNotSupportedException extends SecurityException
{
    public EncryptionNotSupportedException(Message message, UMOMessage umoMessage)
    {
        super(message, umoMessage);
    }

    public EncryptionNotSupportedException(Message message, UMOMessage umoMessage, Throwable cause)
    {
        super(message, umoMessage, cause);
    }
}
