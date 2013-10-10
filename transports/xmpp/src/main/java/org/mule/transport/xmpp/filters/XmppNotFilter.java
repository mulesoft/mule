/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp.filters;

import org.mule.util.ClassUtils;

import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.PacketFilter;

/**
 * <code>XmppNotFilter</code> an Xmpp NOT filter
 */
public class XmppNotFilter extends AbstractXmppFilter
{
    private volatile PacketFilter filter;

    public XmppNotFilter()
    {
        super();
    }

    public XmppNotFilter(PacketFilter filter)
    {
        this.filter = filter;
    }

    public void setFilter(PacketFilter filter)
    {
        this.filter = filter;
    }

    public PacketFilter getFilter()
    {
        return filter;
    }

    @Override
    protected PacketFilter createFilter()
    {
        return new NotFilter(filter);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final XmppNotFilter other = (XmppNotFilter) obj;
        return ClassUtils.equal(filter, other.filter);
    }

    @Override
    public int hashCode()
    {
        return ClassUtils.hash(new Object[]{this.getClass(), filter});
    }
}
