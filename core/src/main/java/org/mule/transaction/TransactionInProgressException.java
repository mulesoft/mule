/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transaction;

import org.mule.config.i18n.Message;

/**
 * <p>
 * <code>TransactionInProgressException</code> is thrown if a new transaction is
 * started when there is one already in progress.
 */
public class TransactionInProgressException extends TransactionStatusException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6041127507191183323L;

    /**
     * @param message the exception message
     */
    public TransactionInProgressException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public TransactionInProgressException(Message message, Throwable cause)
    {
        super(message, cause);
    }

}
