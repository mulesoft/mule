/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.connectors;

import org.mule.api.transport.Connector;
import org.mule.transport.email.Pop3Connector;

import com.icegreen.greenmail.util.ServerSetup;

/**
 * Simple tests for pulling from a POP3 server.
 */
public class Pop3ConnectorTestCase extends AbstractReceivingMailConnectorTestCase
{

    public Pop3ConnectorTestCase()
    {
        super(ServerSetup.PROTOCOL_POP3);
    }

    public Connector createConnector() throws Exception
    {
        Pop3Connector connector = new Pop3Connector(muleContext);
        connector.setName("Pop3Connector");
        connector.setCheckFrequency(POLL_PERIOD_MS);
        connector.setServiceOverrides(newEmailToStringServiceOverrides());
        return connector;
    }

}
