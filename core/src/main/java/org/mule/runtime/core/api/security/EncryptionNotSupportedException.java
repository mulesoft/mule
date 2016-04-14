/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.security;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
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

    public EncryptionNotSupportedException(Message message, MuleEvent event, Throwable cause, MessageProcessor failingMessageProcessor)
    {
        super(message, event, cause, failingMessageProcessor);
    }
}
