/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.umo.security;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOException;

/**
 * <code>UnknownAuthenticationTypeException</code> is thrown if a security
 * context request is make with an unrecognised UMOAuthentication type.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class UnknownAuthenticationTypeException extends UMOException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6275865761357999175L;

    public UnknownAuthenticationTypeException(UMOAuthentication authentication)
    {
        super(new Message(Messages.AUTH_TYPE_NOT_RECOGNISED, (authentication == null ? "null"
                : authentication.getClass().getName())));
    }
}
