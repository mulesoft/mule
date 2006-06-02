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
package org.mule.umo.model;

import org.mule.config.i18n.Message;
import org.mule.umo.UMOException;

/**
 * @author Ross Mason
 */
public class ModelException extends UMOException
{
    /**
     * @param message the exception message
     */
    public ModelException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public ModelException(Message message, Throwable cause)
    {
        super(message, cause);
    }

}
