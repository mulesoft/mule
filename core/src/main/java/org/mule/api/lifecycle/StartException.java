/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.lifecycle;

import org.mule.config.i18n.Message;

/** <code>DisposeException</code> TODO (document class) */

public class StartException extends LifecycleException
{
    /** Serial version */
    private static final long serialVersionUID = 1714192220605243678L;

    /**
     * @param message   the exception message
     * @param component the object that failed during a lifecycle method call
     */
    public StartException(Message message, Startable component)
    {
        super(message, component);
    }

    /**
     * @param message   the exception message
     * @param cause     the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public StartException(Message message, Throwable cause, Startable component)
    {
        super(message, cause, component);
    }

    /**
     * @param cause     the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public StartException(Throwable cause, Startable component)
    {
        super(cause, component);
    }
}
