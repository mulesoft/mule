/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.sftp;

import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;

import org.junit.Test;

public class SftpConnectorTestCase extends AbstractConnectorTestCase
{

    /*
     * For general guidelines on writing transports see
     * http://www.mulesource.org/display/MULE2USER/Creating+Transports
     */

    public Connector createConnector() throws Exception
    {
        /*
         * IMPLEMENTATION NOTE: Create and initialise an instance of your connector
         * here. Do not actually call the connect method.
         */

        SftpConnector c = new SftpConnector(muleContext);
        c.setName("Test");
        // TODO Set any additional properties on the connector here
        return c;
    }

    public String getTestEndpointURI()
    {
        return "sftp://ms/data";
    }

    public Object getValidMessage() throws Exception
    {
        return "payload";
    }

    @Test
    public void testProperties() throws Exception
    {
        // TODO test setting and retrieving any custom properties on the
        // Connector as necessary
    }

    @Override
    public void testConnectorMessageRequesterFactory()
    {
        // No MessageRequesterFactory
    }

}
