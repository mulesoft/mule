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
 * <code>UnsupportedAuthenticationSchemeException</code> is thrown when a authentication scheme
 * is being used on the message that the Security filter does not understand
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class UnsupportedAuthenticationSchemeException extends SecurityException
{
    public UnsupportedAuthenticationSchemeException(Message message, UMOMessage umoMessage)
    {
        super(message, umoMessage);
    }

    public UnsupportedAuthenticationSchemeException(Message message, UMOMessage umoMessage, Throwable cause)
    {
        super(message, umoMessage, cause);
    }
}
