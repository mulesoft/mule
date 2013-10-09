/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transaction;

import org.mule.api.transaction.TransactionException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;

public class TransactionStatusException extends TransactionException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -2408368544426562868L;

    /**
     * @param message the exception message
     */
    public TransactionStatusException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public TransactionStatusException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public TransactionStatusException(Throwable cause)
    {
        super(CoreMessages.transactionCannotReadState(), cause);
    }
}
