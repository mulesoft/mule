/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.transaction;

import org.mule.config.i18n.Message;


/**
 * <p><code>TransactionNotInProgressException</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class TransactionNotInProgressException extends TransactionStatusException
{
    /**
     * @param message the exception message
     */
    public TransactionNotInProgressException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public TransactionNotInProgressException(Message message, Throwable cause)
    {
        super(message, cause);
    }

}
