/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.rest;

import org.mule.config.i18n.Message;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOMessage;

/**
 * todo document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class RestServiceException extends MessagingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1026055907767407433L;

    public RestServiceException(Message message, UMOMessage umoMessage)
    {
        super(message, umoMessage);
    }

    public RestServiceException(Message message, UMOMessage umoMessage, Throwable cause)
    {
        super(message, umoMessage, cause);
    }
}
