/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.dataintegrity;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.transport.sftp.SftpClient;

import com.jcraft.jsch.SftpException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Verify that the original file is not lost if the password for the outbound
 * endpoint is wrong
 */
public class SftpWrongPassPhraseOnOutboundDirectoryTestCase extends AbstractSftpDataIntegrityTestCase
{
    private static String INBOUND_ENDPOINT_NAME = "inboundEndpoint";

    public SftpWrongPassPhraseOnOutboundDirectoryTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "dataintegrity/sftp-wrong-passphrase-config-service.xml"},
            {ConfigVariant.FLOW, "dataintegrity/sftp-wrong-passphrase-config-flow.xml"}});
    }

    // @Override
    // protected void doSetUp() throws Exception
    // {
    // super.doSetUp();
    //
    // // Delete the in & outbound directories
    // initEndpointDirectory(INBOUND_ENDPOINT_NAME);
    // }

    /**
     * The outbound directory doesn't exist. The source file should still exist
     */
    @Test
    public void testWrongPassPhraseOnOutboundDirectory() throws Exception
    {
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

        SftpClient client = getSftpClient(INBOUND_ENDPOINT_NAME);
        try
        {
            ImmutableEndpoint endpoint = (ImmutableEndpoint) muleContext.getRegistry().lookupObject(INBOUND_ENDPOINT_NAME);
            assertTrue("The inbound file should still exist",
                super.verifyFileExists(client, endpoint.getEndpointURI(), FILENAME));
        }
        finally
        {
            client.disconnect();
        }
    }

    /**
     * Ensures that the directory exists and is writable by deleting the directory
     * and then recreate it. Overrides inherited behavior to use working credentials.
     */
    @Override
    protected void initEndpointDirectory(String endpointName)
        throws MuleException, IOException, SftpException
    {
        // SftpClient sftpClient = getSftpClient(muleClient, endpointName);
        // try
        // {
        // ChannelSftp channelSftp = sftpClient.getChannelSftp();
        // try
        // {
        // recursiveDelete(sftpClient, endpointName, "");
        // }
        // catch (IOException e)
        // {
        // if (logger.isErrorEnabled())
        // logger.error("Failed to recursivly delete endpoint " + endpointName, e);
        // }
        //
        // String path = getPathByEndpoint(sftpClient, endpointName);
        // channelSftp.mkdir(path);
        // }
        // finally
        // {
        // sftpClient.disconnect();
        // if (logger.isDebugEnabled()) logger.debug("Done init endpoint directory: "
        // + endpointName);
        // }
    }
}
