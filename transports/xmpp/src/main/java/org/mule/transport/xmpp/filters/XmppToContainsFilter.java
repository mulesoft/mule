/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp.filters;

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.ToContainsFilter;

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
        return new ToContainsFilter(pattern);
    }
}
