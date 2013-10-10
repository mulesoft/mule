/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp.filters;

import org.mule.util.ClassUtils;

import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;

/**
 * <code>XmppAndFilter</code> an Xmpp AND filter
 */
public class XmppAndFilter extends AbstractXmppFilter
{
    private volatile PacketFilter leftFilter;
    private volatile PacketFilter rightFilter;

    public XmppAndFilter()
    {
        super();
    }

    public XmppAndFilter(PacketFilter left, PacketFilter right)
    {
        this.leftFilter = left;
        this.rightFilter = right;
    }

    public void setLeftFilter(PacketFilter left)
    {
        this.leftFilter = left;
    }

    public void setRightFilter(PacketFilter right)
    {
        this.leftFilter = right;
    }

    public PacketFilter getLeftFilter()
    {
        return leftFilter;
    }

    public PacketFilter getRightFilter()
    {
        return rightFilter;
    }

    @Override
    protected PacketFilter createFilter()
    {
        return new AndFilter(leftFilter, rightFilter);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final XmppAndFilter other = (XmppAndFilter) obj;
        return ClassUtils.equal(leftFilter, other.leftFilter)
            && ClassUtils.equal(rightFilter, other.rightFilter);
    }

    @Override
    public int hashCode()
    {
        return ClassUtils.hash(new Object[]{ this.getClass(), leftFilter, rightFilter });
    }
}
