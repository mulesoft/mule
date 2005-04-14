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

import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.PacketFilter;

/**
 * <code>XmppAndFilter</code> an Xmpp AND filter
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XmppNotFilter extends AbstractXmppFilter {
    private PacketFilter filter;

    public XmppNotFilter() {
    }


    public XmppNotFilter(PacketFilter filter) {
        this.filter = filter;
    }


    public void setFilter(PacketFilter filter) {
        this.filter = filter;
    }


    public PacketFilter getFilter() {
        return filter;
    }

    protected PacketFilter createFilter() {
        return new NotFilter(filter);
    }
}
