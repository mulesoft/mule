/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction;

import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.umo.TransactionException;

public class TransactionRollbackException extends TransactionException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3247455113055556221L;

    /**
     * @param message the exception message
     */
    public TransactionRollbackException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public TransactionRollbackException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public TransactionRollbackException(Throwable cause)
    {
        super(CoreMessages.transactionRollbackFailed(), cause);
    }

}
