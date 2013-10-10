/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
