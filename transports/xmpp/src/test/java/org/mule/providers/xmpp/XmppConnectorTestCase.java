/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.xmpp;

import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

import org.jivesoftware.smack.packet.Message;

public class XmppConnectorTestCase extends AbstractConnectorTestCase
{
    public UMOConnector getConnector() throws Exception
    {
        XmppConnector cnn = new XmppConnector();
        cnn.setName("xmppConnector");
        return cnn;
    }

    public Object getValidMessage() throws Exception
    {
        return new Message("Hello");
    }

    public String getTestEndpointURI()
    {
        return "xmpp://mule1:mule@jabber.org.au/ross@jabber.org";
    }
}
