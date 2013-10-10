/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp.filters;

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.ThreadFilter;

/**
 * <code>XmppThreadFilter</code> is an Xmpp ThreadFilter adapter.
 */
public class XmppThreadFilter extends XmppFromContainsFilter
{
    public XmppThreadFilter()
    {
        super();
    }

    public XmppThreadFilter(String expression)
    {
        super(expression);
    }

    @Override
    protected PacketFilter createFilter()
    {
        return new ThreadFilter(pattern);
    }
}
