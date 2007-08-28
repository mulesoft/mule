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

import org.mule.providers.email.SmtpsConnector;
import org.mule.umo.provider.UMOConnector;

public class SmtpsConnectorTestCase extends SmtpConnectorTestCase
{
    
    public SmtpsConnectorTestCase()
    {
        super("SmtpsConnector");
    }
    
    // @Override
    public UMOConnector createConnector(boolean init) throws Exception
    {
        SmtpsConnector connector = new SmtpsConnector();
        connector.setName(getConnectorName());
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
        return getSmtpsTestEndpointURI();
    }

}
