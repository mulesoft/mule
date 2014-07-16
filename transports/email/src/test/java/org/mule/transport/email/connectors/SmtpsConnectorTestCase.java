/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
