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

package org.mule.impl;

import org.mule.MuleException;

/**
 * <code>SessionException</code> is thrown when errors occur in the MuleSession
 * or Seession Manager
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class SessionException extends MuleException
{
    /**
     * @param message the exception payload
     */
    public SessionException(String message)
    {
        super(message);
    }

    /**
     * @param message the exception payload
     * @param cause   the exception that triggered this exception
     */
    public SessionException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
