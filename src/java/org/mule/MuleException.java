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
package org.mule;

import org.mule.umo.UMOException;
import org.mule.config.i18n.Message;

/**
 * <code>MuleException</code> Is the base exception type for the Mule application
 * any other exceptions thrown by Mule code will be based on this exception
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class MuleException extends UMOException
{
    /**
     * @param message the exception message
     */
    public MuleException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public MuleException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public MuleException(Throwable cause) {
        super(cause);
    }
}