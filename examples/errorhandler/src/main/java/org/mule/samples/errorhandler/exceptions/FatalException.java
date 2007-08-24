/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.errorhandler.exceptions;

import org.mule.config.i18n.MessageFactory;
import org.mule.umo.UMOException;

/**
 * <code>FatalException</code> TODO (document class)
 */
public class FatalException extends UMOException
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = -5683879269751770579L;

    /**
     * @param message
     */
    public FatalException(String message)
    {
        this("FATAL EXCEPTION: " + message, null);
    }

    /**
     * @param message
     * @param cause
     */
    public FatalException(String message, Throwable cause)
    {
        super(MessageFactory.createStaticMessage(message), cause);
    }

}
