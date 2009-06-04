/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import org.mule.tck.FunctionalTestCase;
import org.mule.transport.file.DummyFilenameParser;
import org.mule.transport.file.FilenameParser;
import org.mule.transport.ftp.FtpConnector;

/**
 * Load a mule config and verify that the parameters are set as expected
 */
public class FtpNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "ftp-namespace-config.xml";
    }

    public void testConfig() throws Exception
    {
        FtpConnector c = (FtpConnector)muleContext.getRegistry().lookupConnector("ftpConnector");
        assertNotNull(c);

        assertEquals("abc", c.getOutputPattern());
        assertEquals(1234, c.getPollingFrequency());
        assertEquals(false, c.isBinary());
        assertEquals(false, c.isPassive());
        assertEquals(false, c.isValidateConnections());
        assertEquals(FTPConnectorTestCase.TestFtpConnectionFactory.class.getName(), c.getConnectionFactoryClass());

        FilenameParser parser = c.getFilenameParser();
        assertTrue(parser.getClass().getName(), c.getFilenameParser() instanceof DummyFilenameParser);

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }
}
