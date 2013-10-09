/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.connectors;

import org.mule.api.transport.Connector;
import org.mule.transport.email.ImapConnector;

import com.icegreen.greenmail.util.ServerSetup;

/**
 * Simple tests for pulling from an IMAP server.
 */
public class ImapConnectorTestCase extends AbstractReceivingMailConnectorTestCase
{
    public ImapConnectorTestCase()
    {
        super(ServerSetup.PROTOCOL_IMAP);
    }

    @Override
    public Connector createConnector() throws Exception
    {
        ImapConnector connector = new ImapConnector(muleContext);
        connector.setName("ImapConnector");
        connector.setCheckFrequency(POLL_PERIOD_MS);
        connector.setServiceOverrides(newEmailToStringServiceOverrides());
        return connector;
    }

}
