/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.xmpp.filters;

import org.jivesoftware.smack.filter.PacketFilter;

/**
 * <code>XmppPacketTypeFilter</code> is an Xmpp PacketTypeFilter adapter.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XmppPacketTypeFilter extends AbstractXmppFilter
{
    private Class expectedType;

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
}
