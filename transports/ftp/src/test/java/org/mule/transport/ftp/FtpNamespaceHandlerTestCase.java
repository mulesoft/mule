/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.endpoint.EndpointException;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.file.DummyFilenameParser;
import org.mule.transport.file.FilenameParser;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.junit.Test;

/**
 * Load a mule config and verify that the parameters are set as expected
 */
public class FtpNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "ftp-namespace-config.xml";
    }

    @Test
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
    
    @Test
    public void testReceiverFtpConnector() throws EndpointException
    {
        FtpConnector c = (FtpConnector)muleContext.getRegistry().lookupConnector("receiverFtpConnector");
        assertNotNull(c);
        
        MuleEndpointURI uri = new MuleEndpointURI("http://localhost", null);
        GenericObjectPool objectPool = (GenericObjectPool) c.getFtpPool(uri);
        assertEquals(GenericObjectPool.WHEN_EXHAUSTED_FAIL, objectPool.getWhenExhaustedAction());
    }
}
