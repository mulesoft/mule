/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.jivesoftware.smack.packet.Message;


/**
 * This enum represents the XMPP message types that can be used in Mule.
 */
public enum XmppMessageType
{
    /**
     * represents messages of type {@link Message.Type.normal}
     */
    MESSAGE,

    /**
     * represents messages of type {@link Message.Type.chat}
     */
    CHAT,

    /**
     * represents messages of type {@link Message.Type.groupchat}
     */
    GROUPCHAT,

    /**
     * dynamic Mule endpoints use this identifier
     */
    dynamic
}
