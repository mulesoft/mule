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

package org.mule.umo;

import org.mule.config.i18n.Message;

/**
 * <p><code>TransactionException</code> is thrown when an exception occurs
 * while trying to create, start commit or rollback an exception
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TransactionException extends UMOException
{
    /**
     * @param message the exception message
     */
    public TransactionException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
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
