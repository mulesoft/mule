/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.lifecycle;

import org.mule.config.i18n.Message;

/**
 * <code>DisposeException</code> TODO (document class)
 */

public class DisposeException extends LifecycleException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1714192290605243678L;

    /**
     * @param message the exception message
     * @param component the object that failed during a lifecycle method call
     */
    public DisposeException(Message message, Object component)
    {
        super(message, component);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public DisposeException(Message message, Throwable cause, Object component)
    {
        super(message, cause, component);
    }

    /**
     * @param cause the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public DisposeException(Throwable cause, Object component)
    {
        super(cause, component);
    }
}
