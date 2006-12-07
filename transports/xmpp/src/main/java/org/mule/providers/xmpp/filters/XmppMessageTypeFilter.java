/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.xmpp.filters;

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

    protected PacketFilter createFilter()
    {
        return new org.jivesoftware.smack.filter.MessageTypeFilter(Message.Type.fromString(pattern));
    }
}
