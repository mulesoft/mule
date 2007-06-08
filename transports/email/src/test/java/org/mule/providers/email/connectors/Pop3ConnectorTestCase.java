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

import org.mule.providers.email.Pop3Connector;
import org.mule.umo.provider.UMOConnector;

/**
 * Simple tests for pulling from a POP3 server.
 */
public class Pop3ConnectorTestCase extends AbstractReceivingMailConnectorTestCase
{

    public Pop3ConnectorTestCase()
    {
        super("Pop3Connector");
    }
    
    public UMOConnector getConnector(boolean init) throws Exception
    {
        Pop3Connector connector = new Pop3Connector();
        connector.setName(getConnectorName());
        connector.setCheckFrequency(POLL_PERIOD_MS);
        connector.setServiceOverrides(newEmailToStringServiceOverrides());
        if (init)
        {
            connector.initialise();
        }
        return connector;
    }

    public String getTestEndpointURI()
    {
        return getPop3TestEndpointURI();
    }

}
