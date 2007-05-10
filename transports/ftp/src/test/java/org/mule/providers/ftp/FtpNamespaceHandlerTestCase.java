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

import org.mule.tck.FunctionalTestCase;
import org.mule.providers.file.FilenameParser;
import org.mule.providers.file.DummyFilenameParser;

import junit.framework.Assert;

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

        Assert.assertEquals("abc", c.getOutputPattern());
        Assert.assertEquals(1234, c.getPollingFrequency());
        Assert.assertEquals(false, c.isBinary());
        Assert.assertEquals(false, c.isPassive());
        Assert.assertEquals(false, c.isValidateConnections());

        FilenameParser parser = c.getFilenameParser();
        assertTrue(parser.getClass().getName(), c.getFilenameParser() instanceof DummyFilenameParser);

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

}