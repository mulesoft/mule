/* 
 * $Header$
 * $Revision$
 * $Date$
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
 * <p><code>IllegalTransactionStateException</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class IllegalTransactionStateException extends TransactionStatusException
{
    /**
     * @param message the exception message
     */
    public IllegalTransactionStateException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public IllegalTransactionStateException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
