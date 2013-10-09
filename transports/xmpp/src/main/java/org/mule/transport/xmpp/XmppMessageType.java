/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
