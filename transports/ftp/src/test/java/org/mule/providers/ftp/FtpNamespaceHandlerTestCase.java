/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.ftp;

import org.mule.providers.file.DummyFilenameParser;
import org.mule.providers.file.FilenameParser;
import org.mule.tck.FunctionalTestCase;

public class FtpNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "ftp-namespace-config.xml";
    }

    public void testConfig() throws Exception
    {
        FtpConnector c = (FtpConnector)managementContext.getRegistry().lookupConnector("ftpConnector");
        assertNotNull(c);

        assertEquals("abc", c.getOutputPattern());
        assertEquals(1234, c.getPollingFrequency());
        assertEquals(false, c.isBinary());
        assertEquals(false, c.isPassive());
        assertEquals(false, c.isValidateConnections());

        FilenameParser parser = c.getFilenameParser();
        assertTrue(parser.getClass().getName(), c.getFilenameParser() instanceof DummyFilenameParser);

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

}