/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.security;

import org.mule.config.i18n.CoreMessages;
import org.mule.umo.UMOException;

/**
 * <code>UnknownAuthenticationTypeException</code> is thrown if a security context
 * request is make with an unrecognised UMOAuthentication type.
 */

public class UnknownAuthenticationTypeException extends UMOException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6275865761357999175L;

    public UnknownAuthenticationTypeException(UMOAuthentication authentication)
    {
        super(CoreMessages.authTypeNotRecognised((authentication == null
                        ? "null" : authentication.getClass().getName())));
    }
}
