/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp.filters;

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;

/**
 * <code>XmppToContainsFilter</code> is an Xmpp ToContainsfilter adapter.
 */
public class XmppToContainsFilter extends XmppFromContainsFilter
{
    public XmppToContainsFilter()
    {
        super();
    }

    public XmppToContainsFilter(String expression)
    {
        super(expression);
    }

    @Override
    protected PacketFilter createFilter()
    {
        return new PacketFilter() {

            @Override
            public boolean accept(Packet packet) {
                String to = packet.getTo();
                if (to == null) {
                    return false;
                }
                return to.contains(pattern);
            }

        };
    }
}
