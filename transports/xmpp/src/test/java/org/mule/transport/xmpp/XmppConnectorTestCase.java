/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;

import org.jivesoftware.smack.packet.Message;

public class XmppConnectorTestCase extends AbstractConnectorTestCase
{
    @Override
    protected boolean isDisabledInThisEnvironment()
    {
        return XmppEnableDisableTestCase.isTestDisabled();
    }

    @Override
    public Connector createConnector() throws Exception
    {
        XmppConnector connector = new XmppConnector(muleContext);
        connector.setName("xmppConnector");
        connector.setHost("localhost");
        connector.setUser("mule1");
        connector.setPassword("mule");
        return connector;
    }

    @Override
    public Object getValidMessage() throws Exception
    {
        Message message = new Message("ross@jabber.org");
        message.setBody("Hello");
        return message;
    }

    protected String getProtocol()
    {
        return "xmpp";
    }

    @Override
    public String getTestEndpointURI()
    {
        return getProtocol() + "://MESSAGE/mule1@localhost";
    }
}
