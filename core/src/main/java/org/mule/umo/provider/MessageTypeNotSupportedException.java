/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.umo.provider;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.MessagingException;

/**
 * <code>MessageTypeNotSupportedException</code> is thrown when a message payload
 * is set on a Message implementation of MessageAdapter which is not of supported
 * type for that message or adapter.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class MessageTypeNotSupportedException extends MessagingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3954838511333933643L;

    public MessageTypeNotSupportedException(Object message, Class adapterClass)
    {
        super(new Message(Messages.MESSAGE_X_NOT_SUPPORTED_BY_ADAPTER_X,
                          (message != null ? message.getClass().getName() : "null"),
                          (adapterClass != null ? adapterClass.getName() : "null class")),
                          message);
    }

    public MessageTypeNotSupportedException(Object message, Class adapterClass, Throwable cause)
    {
        super(new Message(Messages.MESSAGE_X_NOT_SUPPORTED_BY_ADAPTER_X,
                (message != null ? message.getClass().getName() : "null"),
                (adapterClass != null ? adapterClass.getName() : "null class")),
                message, cause);
    }

}
