/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.lifecycle;

import org.mule.config.i18n.Message;

/**
 * <code>RecoverableException</code> can be thrown during initialisation to
 * indicate that the error occurred is not fatal and a reactive action can be
 * performed to try and remedy the error. The most common example would be a
 * Connector failing to connect due to a JVM_BIND exception.
 */
public class RecoverableException extends InitialisationException
{

    /** Serial version */
    private static final long serialVersionUID = -5799024626172482665L;

    /** @param message the exception message */
    public RecoverableException(Message message, Initialisable object)
    {
        super(message, object);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public RecoverableException(Message message, Initialisable object, Throwable cause)
    {
        super(message, cause, object);
    }

}
