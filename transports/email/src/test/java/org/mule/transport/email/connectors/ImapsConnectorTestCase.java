/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.connectors;

import org.mule.api.transport.Connector;
import org.mule.transport.email.ImapsConnector;

import com.icegreen.greenmail.util.ServerSetup;

/**
 * Simple tests for pulling from an IMAP server.
 */
public class ImapsConnectorTestCase extends AbstractReceivingMailConnectorTestCase
{
    public ImapsConnectorTestCase()
    {
        super(ServerSetup.PROTOCOL_IMAPS);
    }

    public Connector createConnector() throws Exception
    {
        ImapsConnector connector = new ImapsConnector(muleContext);
        connector.setName("ImapsConnector");
        connector.setCheckFrequency(POLL_PERIOD_MS);
        connector.setServiceOverrides(newEmailToStringServiceOverrides());
        connector.setTrustStore("greenmail.jks");
        connector.setTrustStorePassword("changeit");
        return connector;
    }
}
