/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.xmpp.filters;

import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;

/**
 * <code>XmppAndFilter</code> an Xmpp AND filter
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XmppAndFilter extends AbstractXmppFilter
{
    private PacketFilter leftFilter;
    private PacketFilter rightFilter;

    public XmppAndFilter()
    {
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

    protected PacketFilter createFilter()
    {
        return new AndFilter(leftFilter, rightFilter);
    }
}
