/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp.filters;

import org.mule.util.ClassUtils;

import org.jivesoftware.smack.filter.PacketFilter;

/**
 * <code>XmppPacketTypeFilter</code> is an Xmpp PacketTypeFilter adapter.
 */
public class XmppPacketTypeFilter extends AbstractXmppFilter
{
    private volatile Class<?> expectedType;

    public XmppPacketTypeFilter()
    {
        super();
    }

    public XmppPacketTypeFilter(Class<?> expectedType)
    {
        setExpectedType(expectedType);
    }

    public Class<?> getExpectedType()
    {
        return expectedType;
    }

    public void setExpectedType(Class<?> expectedType)
    {
        this.expectedType = expectedType;
    }

    @Override
    protected PacketFilter createFilter()
    {
        return new org.jivesoftware.smack.filter.PacketTypeFilter(expectedType);
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        XmppPacketTypeFilter other = (XmppPacketTypeFilter) obj;
        return ClassUtils.equal(expectedType, other.expectedType);
    }

    @Override
    public int hashCode()
    {
        return ClassUtils.hash(new Object[]{this.getClass(), expectedType});
    }
}
