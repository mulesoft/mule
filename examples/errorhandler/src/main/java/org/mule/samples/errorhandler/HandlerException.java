/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.errorhandler;

import org.mule.config.i18n.MessageFactory;
import org.mule.umo.UMOException;

public class HandlerException extends UMOException
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = -1446892313657626797L;

    public HandlerException(String message)
    {
        super(MessageFactory.createStaticMessage(message));
    }

    public HandlerException(String message, Throwable cause)
    {
        super(MessageFactory.createStaticMessage(message), cause);
    }

}
