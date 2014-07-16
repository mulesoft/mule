/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.security;

import org.mule.api.MuleEvent;
import org.mule.config.i18n.Message;

/**
 * <code>UnsupportedAuthenticationSchemeException</code> is thrown when a
 * authentication scheme is being used on the message that the Security filter does
 * not understand
 */
public class UnsupportedAuthenticationSchemeException extends SecurityException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 3281021140543598681L;

    public UnsupportedAuthenticationSchemeException(Message message, MuleEvent event)
    {
        super(message, event);
    }

    public UnsupportedAuthenticationSchemeException(Message message, MuleEvent event, Throwable cause)
    {
        super(message, event, cause);
    }
}
