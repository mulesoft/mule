/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
