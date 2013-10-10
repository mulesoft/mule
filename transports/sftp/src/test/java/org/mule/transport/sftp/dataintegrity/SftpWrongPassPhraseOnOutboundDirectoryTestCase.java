/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.dataintegrity;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.transport.sftp.SftpClient;

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Verify that the original file is not lost if the password for the outbound
 * endpoint is wrong
 */
public class SftpWrongPassPhraseOnOutboundDirectoryTestCase extends AbstractSftpDataIntegrityTestCase
{

    private static String INBOUND_ENDPOINT_NAME = "inboundEndpoint";

    @Override
    protected String getConfigResources()
    {
        return "dataintegrity/sftp-wrong-passphrase-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        // Delete the in & outbound directories
        initEndpointDirectory(INBOUND_ENDPOINT_NAME);
    }

    /**
     * The outbound directory doesn't exist. The source file should still exist
     * 
     * @throws Exception
     */
    @Test
    public void testWrongPassPhraseOnOutboundDirectory() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);
        assertTrue(muleContext.isStarted());
        // Send an file to the SFTP server, which the inbound-outboundEndpoint then
        // can pick up
        final Exception exception = dispatchAndWaitForException(new DispatchParameters(INBOUND_ENDPOINT_NAME,
            null), "sftp", "service");
        assertNotNull(exception);
        assertTrue("expected DispatchException, but got " + exception.getClass().toString(),
            exception instanceof DispatchException);
        assertTrue("expected IOException, but got " + exception.getCause().getClass().toString(),
            exception.getCause() instanceof IOException);
        assertTrue("wrong message : " + exception.getCause().getMessage(), exception.getCause()
            .getMessage()
            .startsWith("Error during login to"));

        SftpClient sftpClient = getSftpClient(muleClient, INBOUND_ENDPOINT_NAME);
        try
        {
            ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(INBOUND_ENDPOINT_NAME);
            assertTrue("The inbound file should still exist", super.verifyFileExists(sftpClient,
                endpoint.getEndpointURI(), FILE_NAME));
        }
        finally
        {
            sftpClient.disconnect();
        }
    }

}
