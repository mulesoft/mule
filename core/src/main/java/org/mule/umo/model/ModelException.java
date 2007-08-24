/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.model;

import org.mule.config.i18n.Message;
import org.mule.umo.UMOException;

public class ModelException extends UMOException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -4593985962209518596L;

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
