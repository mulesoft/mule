/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.lifecycle;

import org.mule.config.i18n.Message;

/**
 * <code>InitialisationException</code> is thrown by the initialise method defined
 * in the <code>org.mule.api.lifecycle.Initialisable</code> interface.
 * IinitialisationExceptions are fatal and will cause the current Mule instance to
 * shutdown.
 */
public class InitialisationException extends LifecycleException
{
    /** Serial version */
    private static final long serialVersionUID = -8402348927606781931L;

    /**
     * @param message   the exception message
     * @param component the object that failed during a lifecycle method call
     */
    public InitialisationException(Message message, Initialisable component)
    {
        super(message, component);
    }

    /**
     * @param message   the exception message
     * @param cause     the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public InitialisationException(Message message, Throwable cause, Initialisable component)
    {
        super(message, cause, component);
    }

    /**
     * @param cause     the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public InitialisationException(Throwable cause, Initialisable component)
    {
        super(cause, component);
    }
}
