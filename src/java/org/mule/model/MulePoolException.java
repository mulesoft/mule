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
package org.mule.model;

import org.mule.MuleException;

/**
 * <code>MulePoolException</code> is thrown Proxy pool implementations when components cannot be
 * instanciated, taken or returned to the pool.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class MulePoolException extends MuleException
{

    /**
     * @param message
     */
    public MulePoolException(String message)
    {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public MulePoolException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
