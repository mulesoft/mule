/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
