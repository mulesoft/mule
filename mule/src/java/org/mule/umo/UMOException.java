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
package org.mule.umo;


/**
 * <code>UMOException</code> is the base exception type for the Mule server
 * any other exceptions thrown by Mule code will be based on this exception
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public abstract class UMOException extends Exception
{
    /**
     * @param message the exception message
     */
    public UMOException(String message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public UMOException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
