/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
