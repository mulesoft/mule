/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ftp;

import org.mule.api.endpoint.EndpointException;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.file.DummyFilenameParser;
import org.mule.transport.file.FilenameParser;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Load a mule config and verify that the parameters are set as expected
 */
public class FtpNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
