/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp.filters;

import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketFilter;

/**
 * <code>XmppOrFilter</code> an Xmpp OR filter
 */
public class XmppOrFilter extends XmppAndFilter
{
    public XmppOrFilter()
    {
        super();
    }

    public XmppOrFilter(PacketFilter left, PacketFilter right)
    {
        super(left, right);
    }

    @Override
    protected PacketFilter createFilter()
    {
        return new OrFilter(getLeftFilter(), getRightFilter());
    }
}
