/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transaction;

import org.mule.api.MuleException;
import org.mule.config.i18n.Message;

/**
 * <p>
 * <code>TransactionException</code> is thrown when an exception occurs while
 * trying to create, start commit or rollback an exception
 */
public class TransactionException extends MuleException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3470229644235978820L;

    /**
     * @param message the exception message
     */
    public TransactionException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public TransactionException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public TransactionException(Throwable cause)
    {
        super(cause);
    }
}
