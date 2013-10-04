/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
