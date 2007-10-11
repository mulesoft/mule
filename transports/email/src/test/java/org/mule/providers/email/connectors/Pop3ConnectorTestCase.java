/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.connectors;

import org.mule.providers.email.Pop3Connector;
import org.mule.umo.provider.UMOConnector;

import com.icegreen.greenmail.util.ServerSetup;

/**
 * Simple tests for pulling from a POP3 server.
 */
public class Pop3ConnectorTestCase extends AbstractReceivingMailConnectorTestCase
{

    public Pop3ConnectorTestCase()
    {
        super(ServerSetup.PROTOCOL_POP3, 50010);
    }

    public UMOConnector createConnector() throws Exception
    {
        Pop3Connector connector = new Pop3Connector();
        connector.setName("Pop3Connector");
        connector.setCheckFrequency(POLL_PERIOD_MS);
        connector.setServiceOverrides(newEmailToStringServiceOverrides());
        return connector;
    }

}
