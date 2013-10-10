/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.security;

import org.mule.api.MuleEvent;
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
    private static final long serialVersionUID = -1661059380853528624L;
    
    public EncryptionNotSupportedException(Message message, MuleEvent event)
    {
        super(message, event);
    }

    public EncryptionNotSupportedException(Message message, MuleEvent event, Throwable cause)
    {
        super(message, event, cause);
    }
}
