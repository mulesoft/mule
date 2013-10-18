/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
