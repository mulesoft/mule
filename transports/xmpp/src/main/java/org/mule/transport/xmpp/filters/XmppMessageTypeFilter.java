/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp.filters;

import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;

/**
 * <code>XmppMessageTypeFilter</code> is an Xmpp MessageTypeFilter adapter.
 */
public class XmppMessageTypeFilter extends XmppFromContainsFilter
{
    public XmppMessageTypeFilter()
    {
        super();
    }

    public XmppMessageTypeFilter(String expression)
    {
        super(expression);
    }

    @Override
    protected PacketFilter createFilter()
    {
        return new MessageTypeFilter(Message.Type.fromString(pattern));
    }
}
