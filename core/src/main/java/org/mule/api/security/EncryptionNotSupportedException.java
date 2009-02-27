/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.security;

import org.mule.api.MuleMessage;
import org.mule.config.i18n.Message;

/**
 * <code>EncryptionNotSupportedException</code> is thrown if an algorithm is set in
 * the MULE_USER header but it doesn't match the algorithm set on the security filter
 */

public class EncryptionNotSupportedException extends SecurityException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1661059380853528623L;

    public EncryptionNotSupportedException(Message message, MuleMessage muleMessage)
    {
        super(message, muleMessage);
    }

    public EncryptionNotSupportedException(Message message, MuleMessage muleMessage, Throwable cause)
    {
        super(message, muleMessage, cause);
    }
}
