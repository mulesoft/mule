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
 */
package org.mule.umo.provider;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.MessagingException;

/**
 * <code>MessageTypeNotSupportedException</code> is thrown when a message
 * payload is set on a Message implementation of Message Adapter i not of
 * supported type for that message or adapter.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class MessageTypeNotSupportedException extends MessagingException
{
    public MessageTypeNotSupportedException(Object message, Class adapterClass)
    {
        super(new Message(Messages.MESSAGE_X_NOT_SUPPORTED_BY_ADAPTER_X,
                          message.getClass().getName(),
                          adapterClass.getName()), message);
    }

    public MessageTypeNotSupportedException(Object message, Class adapterClass, Throwable cause)
    {
        super(new Message(Messages.MESSAGE_X_NOT_SUPPORTED_BY_ADAPTER_X,
                          message.getClass().getName(),
                          adapterClass.getName()), message, cause);
    }

}
