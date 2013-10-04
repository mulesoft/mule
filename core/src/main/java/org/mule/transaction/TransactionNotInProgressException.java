/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transaction;

import org.mule.config.i18n.Message;

/**
 * <p>
 * <code>TransactionNotInProgressException</code> TODO (document class)
 */
public class TransactionNotInProgressException extends TransactionStatusException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -4825546092229025015L;

    /**
     * @param message the exception message
     */
    public TransactionNotInProgressException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public TransactionNotInProgressException(Message message, Throwable cause)
    {
        super(message, cause);
    }

}
