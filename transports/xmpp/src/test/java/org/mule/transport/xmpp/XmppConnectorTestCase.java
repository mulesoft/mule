/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.xmpp;

import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;
import org.mule.transport.xmpp.XmppConnector;

import org.jivesoftware.smack.packet.Message;

public class XmppConnectorTestCase extends AbstractConnectorTestCase
{
    // @Override
    public Connector createConnector() throws Exception
    {
        XmppConnector cnn = new XmppConnector();
        cnn.setName("xmppConnector");
        return cnn;
    }

    public Object getValidMessage() throws Exception
    {
        return new Message("Hello");
    }

    protected String getProtocol()
    {
        return "xmpp";
    }
    
    public String getTestEndpointURI()
    {
        return this.getProtocol() + "://mule1:mule@jabber.org.au/ross@jabber.org";
    }

}
