/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
