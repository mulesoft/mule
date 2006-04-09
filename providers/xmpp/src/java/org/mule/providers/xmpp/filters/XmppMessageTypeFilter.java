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

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;

/**
 * <code>XmppMessageTypeFilter</code> is an Xmpp MessageTypeFilter adapter.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XmppMessageTypeFilter extends XmppFromContainsFilter
{
    public XmppMessageTypeFilter()
    {
        super();
    }

    public XmppMessageTypeFilter(String expression)
    {
        super(expression);
    }

    protected PacketFilter createFilter()
    {
        return new org.jivesoftware.smack.filter.MessageTypeFilter(Message.Type.fromString(pattern));
    }
}
