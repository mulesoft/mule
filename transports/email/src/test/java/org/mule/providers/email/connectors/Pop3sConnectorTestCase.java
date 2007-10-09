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

import org.mule.providers.email.Pop3sConnector;
import org.mule.umo.provider.UMOConnector;

/**
 * Simple tests for pulling from an IMAP server.
 */
public class Pop3sConnectorTestCase extends AbstractReceivingMailConnectorTestCase
{
    public UMOConnector createConnector() throws Exception
    {
        Pop3sConnector connector = new Pop3sConnector();
        connector.setName("Pop3sConnector");
        connector.setCheckFrequency(POLL_PERIOD_MS);
        connector.setServiceOverrides(newEmailToStringServiceOverrides());
        connector.setTrustStorePassword("password");
        connector.setTrustStore("greenmail-truststore");
        connector.setManagementContext(managementContext);
        return connector;
    }

    public String getTestEndpointURI()
    {
        return getPop3sTestEndpointURI();
    }

}
