/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.service.TransportFactory;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.provider.UMOConnector;

public class ConnectorFactoryTestCase extends AbstractMuleTestCase
{
    
    private static final int FREQUENCY = 3001;

    public void testCreatePop3Connector() throws Exception
    {
        MuleEndpointURI url = new MuleEndpointURI("pop3://user:password@pop3.lotsofmail.org");
        UMOConnector cnn = TransportFactory.createConnector(url);
        assertTrue(cnn instanceof Pop3Connector);
        Pop3Connector pop3 = (Pop3Connector) cnn;

        url = new MuleEndpointURI("pop3://user:password@pop3.lotsofmail.org:123?checkFrequency=" +  FREQUENCY);
        cnn = TransportFactory.createConnector(url);
        assertTrue(cnn instanceof Pop3Connector);
        pop3 = (Pop3Connector) cnn;
        assertEquals(FREQUENCY, pop3.getCheckFrequency());
    }

    public void testCreateSmtpConnector() throws Exception
    {
        MuleEndpointURI url = new MuleEndpointURI("smtp://user:password@smtp.lotsofmail.org");
        UMOConnector cnn = TransportFactory.createConnector(url);
        assertTrue(cnn instanceof SmtpConnector);
        SmtpConnector smtp = (SmtpConnector) cnn;

        url = new MuleEndpointURI(
            "smtp://user:password@smtp.lotsofmail.org:1023?fromAddress=admin@somewhere.com&subject=Hello");
        cnn = TransportFactory.createConnector(url);
        assertTrue(cnn instanceof SmtpConnector);
        smtp = (SmtpConnector) cnn;

        assertEquals("admin@somewhere.com", smtp.getFromAddress());
        assertEquals("Hello", smtp.getSubject());
    }
}
