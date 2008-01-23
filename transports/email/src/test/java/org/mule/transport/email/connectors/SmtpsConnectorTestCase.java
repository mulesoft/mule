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
import org.mule.transport.email.SmtpsConnector;

import com.icegreen.greenmail.util.ServerSetup;

public class SmtpsConnectorTestCase extends SmtpConnectorTestCase
{

    public SmtpsConnectorTestCase()
    {
        super(ServerSetup.PROTOCOL_SMTPS, 50008);
    }

    // @Override
    public Connector createConnector() throws Exception
    {
        SmtpsConnector connector = new SmtpsConnector();
        connector.setName("SmtpsConnector");
        connector.setTrustStorePassword("password");
        connector.setTrustStore("greenmail-truststore");
        return connector;
    }

}
