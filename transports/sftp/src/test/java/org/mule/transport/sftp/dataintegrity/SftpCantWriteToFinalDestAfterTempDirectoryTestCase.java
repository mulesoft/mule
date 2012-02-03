/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.sftp.dataintegrity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.transport.sftp.SftpClient;

/**
 * Tests that files are not deleted if the final destination is not writable when
 * using a temp directory.
 */
public class SftpCantWriteToFinalDestAfterTempDirectoryTestCase extends AbstractSftpDataIntegrityTestCase
{

    private static String INBOUND_ENDPOINT_NAME = "inboundEndpoint";
    private static String OUTBOUND_ENDPOINT_NAME = "outboundEndpoint";

    public SftpCantWriteToFinalDestAfterTempDirectoryTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "dataintegrity/sftp-dataintegrity-common-with-tempdir-config-service.xml"},
            {ConfigVariant.FLOW, "dataintegrity/sftp-dataintegrity-common-with-tempdir-config-flow.xml"}});
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        // Delete the in & outbound directories
        initEndpointDirectory(INBOUND_ENDPOINT_NAME);
        initEndpointDirectory(OUTBOUND_ENDPOINT_NAME);
    }

    /**
     * No write access on the outbound directory but write access to the TEMP
     * directory. The source file should still exist and no file should exist in the
     * TEMP directory.
     */
    @Test
    public void testCantWriteToFinalDestAfterTempDirectory() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);

        // Must create the temp directory before we change the access rights
        createRemoteDirectory(muleClient, OUTBOUND_ENDPOINT_NAME, "uploading");

        SftpClient sftpClient = getSftpClient(muleClient, OUTBOUND_ENDPOINT_NAME);

        try
        {
            // change the chmod to "dr-x------" on the outbound-directory
            remoteChmod(muleClient, sftpClient, OUTBOUND_ENDPOINT_NAME, 00500);

            // Send an file to the SFTP server, which the inbound-outboundEndpoint
            // then can pick up
            // Expect an error, permission denied
            Exception exception = dispatchAndWaitForException(new DispatchParameters(INBOUND_ENDPOINT_NAME,
                OUTBOUND_ENDPOINT_NAME), "sftp", "service");
            assertNotNull(exception);
            assertTrue("did not receive DispatchException, got : " + exception.getClass().toString(),
                exception instanceof DispatchException);
            assertTrue("did not receive IOException, got : " + exception.getCause().getClass().toString(),
                exception.getCause() instanceof IOException);

            assertEquals("Permission denied", exception.getCause().getMessage());

            verifyInAndOutFiles(muleClient, INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME, true, false);

            ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(OUTBOUND_ENDPOINT_NAME);
            assertFalse("The inbound file should not be left in the TEMP-dir", super.verifyFileExists(
                sftpClient, endpoint.getEndpointURI().getPath() + "/" + TEMP_DIR, FILENAME));
        }
        finally
        {
            sftpClient.disconnect();
        }
    }
}
