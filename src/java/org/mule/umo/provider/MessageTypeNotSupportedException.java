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
 */
package org.mule.umo.provider;

import org.mule.umo.MessageException;

/**
 * <code>MessageTypeNotSupportedException</code> is thrown when a message payload is set
 * on a Message implementation of Message Adapter i not of supported type for that message or
 * adapter.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class MessageTypeNotSupportedException extends MessageException
{
    public MessageTypeNotSupportedException(Object message, Class adapterClass)
    {
        super("Message type: " + message.getClass().getName() + " is not supported by adapter: " + adapterClass.getName());
    }

}
