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
package org.mule.umo.model;

import org.mule.umo.UMOException;

/**
 * @author Ross Mason
 */
public class ModelException extends UMOException
{

    /**
     * @param message
     */
    public ModelException(String message)
    {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public ModelException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
