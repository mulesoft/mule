/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.security;

import org.mule.api.MuleException;
import org.mule.config.i18n.CoreMessages;

/**
 * <code>UnknownAuthenticationTypeException</code> is thrown if a security context
 * request is make with an unrecognised Authentication type.
 */

public class UnknownAuthenticationTypeException extends MuleException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6275865761357999175L;

    public UnknownAuthenticationTypeException(Authentication authentication)
    {
        super(CoreMessages.authTypeNotRecognised((authentication == null
                        ? "null" : authentication.getClass().getName())));
    }
}
