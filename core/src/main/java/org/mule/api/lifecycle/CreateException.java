/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.lifecycle;

import org.mule.config.i18n.Message;

/**
 * <code>CreateException</code> is thrown when creating an object inside Mule wasn't possible due
 * to inconsistent internal state or wrong input.
 */
public class CreateException extends LifecycleException
{
    /** Serial version */
    private static final long serialVersionUID = -8402348927606781921L;

    /**
     * @param message   the exception message
     * @param component the component that failed during a lifecycle method call
     */
    public CreateException(Message message, Object component)
    {
        super(message, component);
    }

    /**
     * @param message   the exception message
     * @param cause     the exception that cause this exception to be thrown
     * @param component the component that failed during a lifecycle method call
     */
    public CreateException(Message message, Throwable cause, Object component)
    {
        super(message, cause, component);
    }

    /**
     * @param cause     the exception that cause this exception to be thrown
     * @param component the component that failed during a lifecycle method call
     */
    public CreateException(Throwable cause, Object component)
    {
        super(cause, component);
    }
}
