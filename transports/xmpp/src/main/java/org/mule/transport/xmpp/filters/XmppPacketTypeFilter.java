/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.xmpp.filters;

import static org.mule.util.ClassUtils.equal;
import static org.mule.util.ClassUtils.hash;

import org.jivesoftware.smack.filter.PacketFilter;

/**
 * <code>XmppPacketTypeFilter</code> is an Xmpp PacketTypeFilter adapter.
 */
public class XmppPacketTypeFilter extends AbstractXmppFilter
{
    private volatile Class expectedType;

    public XmppPacketTypeFilter()
    {
        super();
    }

    public XmppPacketTypeFilter(Class expectedType)
    {
        setExpectedType(expectedType);
    }

    public Class getExpectedType()
    {
        return expectedType;
    }

    public void setExpectedType(Class expectedType)
    {
        this.expectedType = expectedType;
    }

    protected PacketFilter createFilter()
    {
        return new org.jivesoftware.smack.filter.PacketTypeFilter(expectedType);
    }
    
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final XmppPacketTypeFilter other = (XmppPacketTypeFilter) obj;
        return equal(expectedType, other.expectedType);
    }

    public int hashCode()
    {
        return hash(new Object[]{this.getClass(), expectedType});
    }
}
