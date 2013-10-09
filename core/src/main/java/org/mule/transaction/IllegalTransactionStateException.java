/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transaction;

import org.mule.config.i18n.Message;

/**
 * <code>IllegalTransactionStateException</code> TODO (document class)
 */
public class IllegalTransactionStateException extends TransactionStatusException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1062247038945694389L;

    /**
     * @param message the exception message
     */
    public IllegalTransactionStateException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public IllegalTransactionStateException(Message message, Throwable cause)
    {
        super(message, cause);
    }

}
