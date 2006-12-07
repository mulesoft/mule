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

    protected PacketFilter createFilter()
    {
        return new org.jivesoftware.smack.filter.PacketIDFilter(pattern);
    }
}
