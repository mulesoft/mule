/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.model;

import org.mule.config.i18n.Message;

/**
 * <code>SessionException</code> is thrown when errors occur in the MuleSession or
 * Seession Manager
 */
public class SessionException extends ModelException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6751481096543965553L;

    /**
     * @param message the exception message
     */
    public SessionException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public SessionException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
