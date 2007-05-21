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
import org.mule.providers.email.SmtpsConnector;

public class SmtpsConnectorTestCase extends SmtpConnectorTestCase
{
    
    public SmtpsConnectorTestCase()
    {
        super("SmtpsConnector");
    }
    
    // @Override
    public UMOConnector getConnector(boolean init) throws Exception
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
