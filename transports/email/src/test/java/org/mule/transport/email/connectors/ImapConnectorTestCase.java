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
import org.mule.transport.email.ImapConnector;

import com.icegreen.greenmail.util.ServerSetup;

/**
 * Simple tests for pulling from an IMAP server.
 */
public class ImapConnectorTestCase extends AbstractReceivingMailConnectorTestCase
{

    public ImapConnectorTestCase()
    {
        this(50012);
    }

    public ImapConnectorTestCase(int port)
    {
        super(ServerSetup.PROTOCOL_IMAP, port);
    }

    @Override
    public Connector createConnector() throws Exception
    {
        ImapConnector connector = new ImapConnector();
        connector.setName("ImapConnector");
        connector.setCheckFrequency(POLL_PERIOD_MS);
        connector.setServiceOverrides(newEmailToStringServiceOverrides());
        return connector;
    }

}
