/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp.filters;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;

/**
 * <code>AbstractXmppFilter</code> is a filter adapter so that Smack Filters can be
 * configured as Mule filters.
 */
public abstract class AbstractXmppFilter implements Filter, PacketFilter
{
    protected volatile PacketFilter delegate;

    public boolean accept(Packet packet)
    {
        if (delegate == null)
        {
            delegate = createFilter();
        }

        return delegate.accept(packet);
    }

    public boolean accept(MuleMessage message)
    {
        // If we have received a MuleMessage the filter has already been applied
        return true;
    }

    protected abstract PacketFilter createFilter();
}
