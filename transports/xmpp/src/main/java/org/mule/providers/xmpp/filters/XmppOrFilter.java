/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.xmpp.filters;

import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketFilter;

/**
 * <code>XmppAndFilter</code> an Xmpp OR filter
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

    protected PacketFilter createFilter()
    {
        return new OrFilter(getLeftFilter(), getRightFilter());
    }
}
