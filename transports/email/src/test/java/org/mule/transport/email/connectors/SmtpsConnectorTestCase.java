/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.connectors;

import org.mule.api.transport.Connector;
import org.mule.transport.email.SmtpsConnector;

import com.icegreen.greenmail.util.ServerSetup;

public class SmtpsConnectorTestCase extends SmtpConnectorTestCase
{
    public SmtpsConnectorTestCase()
    {
        super(NO_INITIAL_EMAIL, ServerSetup.PROTOCOL_SMTPS);
        setStartContext(true);
    }

    @Override
    public Connector createConnector() throws Exception
    {
        SmtpsConnector connector = new SmtpsConnector(muleContext);
        connector.setName("SmtpsConnector");
        connector.setTrustStore("greenmail.jks");
        connector.setTrustStorePassword("changeit");
        return connector;
    }
}
