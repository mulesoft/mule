/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.connectors;

import org.mule.umo.provider.UMOConnector;
import org.mule.providers.email.ImapsConnector;

/**
 * Simple tests for pulling from an IMAP server.
 */
public class ImapsConnectorTestCase extends AbstractReceivingMailConnectorTestCase
{
    
    public ImapsConnectorTestCase()
    {
        super("ImapsConnector");
    }

    public UMOConnector getConnector(boolean init) throws Exception
    {
        ImapsConnector connector = new ImapsConnector();
        connector.setName(getConnectorName());
        connector.setCheckFrequency(POLL_PERIOD_MS);
        connector.setServiceOverrides(newEmailToStringServiceOverrides());
        connector.setTrustStorePassword("password");
        connector.setTrustStore("greenmail-truststore");
        if (init) 
        {
            connector.initialise();
        }
        return connector;
    }

    public String getTestEndpointURI()
    {
        return getImapsTestEndpointURI();
    }

}
