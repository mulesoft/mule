/* 
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.transaction;

import org.mule.config.i18n.Message;

/**
 * <p>
 * <code>TransactionInProgressException</code> is thrown if a new transaction
 * is started when there is one already in progress
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
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
