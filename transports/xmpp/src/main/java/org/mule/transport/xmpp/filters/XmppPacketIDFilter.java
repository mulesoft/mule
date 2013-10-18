/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp.filters;

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;

/**
 * <code>XmppPacketIDFilter</code> is an Xmpp PacketIDFilter adapter.
 */
public class XmppPacketIDFilter extends XmppFromContainsFilter
{
    public XmppPacketIDFilter()
    {
        super();
    }

    public XmppPacketIDFilter(String expression)
    {
        super(expression);
    }

    @Override
    protected PacketFilter createFilter()
    {
        return new PacketIDFilter(pattern);
    }
}
