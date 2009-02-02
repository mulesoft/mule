/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.api.transport.MessageReceiver;
import org.mule.config.i18n.Message;

/** 
 * When this exception is thrown it will trigger a retry (reconnection) policy to go into effect if one is configured.
 */
public class ReceiverConnectException extends ConnectException
{
    public ReceiverConnectException(Message message, MessageReceiver receiver)
    {
        super(message, receiver.getReceiverKey());
    }

    public ReceiverConnectException(Message message, Throwable cause, MessageReceiver receiver)
    {
        super(message, cause, receiver.getReceiverKey());
    }

    public ReceiverConnectException(Throwable cause, MessageReceiver receiver)
    {
        super(cause, receiver.getReceiverKey());
    }
}
