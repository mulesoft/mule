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
 * <code>XmppFromContainsFilter</code> is an Xmpp FromContainsfilter adapter.
 */
public class XmppFromContainsFilter extends AbstractXmppFilter
{
    protected volatile String pattern;

    public XmppFromContainsFilter()
    {
        super();
    }

    public XmppFromContainsFilter(String pattern)
    {
        this.pattern = pattern;
    }

    public String getPattern()
    {
        return pattern;
    }

    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

    @Override
    protected PacketFilter createFilter()
    {
        return new XmppFromContainsFilter(pattern);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final XmppFromContainsFilter other = (XmppFromContainsFilter) obj;
        return ClassUtils.equal(pattern, other.pattern);
    }

    @Override
    public int hashCode()
    {
        return ClassUtils.hash(new Object[]{ getClass(), pattern });
    }
}
