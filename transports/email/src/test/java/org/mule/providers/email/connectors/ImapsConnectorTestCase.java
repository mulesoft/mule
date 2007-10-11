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

import org.mule.providers.email.ImapsConnector;
import org.mule.umo.provider.UMOConnector;

import com.icegreen.greenmail.util.ServerSetup;

/**
 * Simple tests for pulling from an IMAP server.
 */
public class ImapsConnectorTestCase extends AbstractReceivingMailConnectorTestCase
{

    public ImapsConnectorTestCase()
    {
        super(ServerSetup.PROTOCOL_IMAPS, 50011);
    }

    public UMOConnector createConnector() throws Exception
    {
        ImapsConnector connector = new ImapsConnector();
        connector.setName("ImapsConnector");
        connector.setCheckFrequency(POLL_PERIOD_MS);
        connector.setServiceOverrides(newEmailToStringServiceOverrides());
        connector.setTrustStorePassword("password");
        connector.setTrustStore("greenmail-truststore");
        return connector;
    }

}
