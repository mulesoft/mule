/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.lifecycle;

import org.mule.config.i18n.Message;

/** <code>DisposeException</code> TODO (document class) */

public class StopException extends LifecycleException
{
    /** Serial version */
    private static final long serialVersionUID = 1714192220605243678L;

    /**
     * @param message   the exception message
     * @param component the object that failed during a lifecycle method call
     */
    public StopException(Message message, Stoppable component)
    {
        super(message, component);
    }

    /**
     * @param message   the exception message
     * @param cause     the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public StopException(Message message, Throwable cause, Stoppable component)
    {
        super(message, cause, component);
    }

    /**
     * @param cause     the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public StopException(Throwable cause, Stoppable component)
    {
        super(cause, component);
    }
}