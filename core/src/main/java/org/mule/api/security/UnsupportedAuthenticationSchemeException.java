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
