/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email.connectors;

import org.mule.api.transport.Connector;
import org.mule.transport.email.Pop3sConnector;

import com.icegreen.greenmail.util.ServerSetup;

/**
 * Simple tests for pulling from an IMAP server.
 */
public class Pop3sConnectorTestCase extends AbstractReceivingMailConnectorTestCase
{

    public Pop3sConnectorTestCase()
    {
        super(ServerSetup.PROTOCOL_POP3S, 50009);
    }

    public Connector createConnector() throws Exception
    {
        Pop3sConnector connector = new Pop3sConnector();
        connector.setName("Pop3sConnector");
        connector.setCheckFrequency(POLL_PERIOD_MS);
        connector.setServiceOverrides(newEmailToStringServiceOverrides());
        connector.setTrustStorePassword("password");
        connector.setTrustStore("greenmail-truststore");
        connector.setMuleContext(muleContext);
        return connector;
    }

}
