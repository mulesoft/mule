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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.transport.sftp.SftpClient;

/**
 * Tests that files are not deleted if the temp directory can't be created
 */
public class SftpCantCreateTempDirectoryTestCase extends AbstractSftpDataIntegrityTestCase
{
    private static String INBOUND_ENDPOINT_NAME = "inboundEndpoint";
    private static String OUTBOUND_ENDPOINT_NAME = "outboundEndpoint";

    public SftpCantCreateTempDirectoryTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "dataintegrity/sftp-dataintegrity-common-with-tempdir-config-service.xml"},
            {ConfigVariant.FLOW, "dataintegrity/sftp-dataintegrity-common-with-tempdir-config-flow.xml"}
        });
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
     * No write access on the outbound directory and thus the TEMP directory cant be
     * created. The source file should still exist
     *
     * @throws Exception If an error occurred
     */
    @Test
    public void testCantCreateTempDirectory() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);

        SftpClient sftpClient = getSftpClient(muleClient, OUTBOUND_ENDPOINT_NAME);

        try
        {
            // change the chmod to "dr-x------" on the outbound-directory
            // --> the temp directory should not be able to be created
            remoteChmod(muleClient, sftpClient, OUTBOUND_ENDPOINT_NAME, 00500);

            // Send an file to the SFTP server, which the inbound-outboundEndpoint
            // then can pick up
            // Expect an error, permission denied
            Exception exception = dispatchAndWaitForException(new DispatchParameters(INBOUND_ENDPOINT_NAME,
                OUTBOUND_ENDPOINT_NAME), "sftp", "service");
            assertNotNull(exception);
            assertTrue("expected DispatchException, but got : " + exception.getClass().toString(),
                exception instanceof DispatchException);
            assertTrue("expected IOException, but got : " + exception.getCause().getClass().toString(),
                exception.getCause() instanceof IOException);
            assertEquals("Could not create the directory 'uploading', caused by: Permission denied",
                exception.getCause().getMessage());

            verifyInAndOutFiles(muleClient, INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME, true, false);
        }
        finally
        {
            sftpClient.disconnect();
        }
    }

    /**
     * The same test as above, but with the difference that this time it should be
     * okay to create the directory, and the file should be gone from the inbound
     * directory.
     */
    // Works, but this is more or less the same test as SftpTempDirFunctionalTestCase
    // so don't use this
    // @Test
    //public void testCanCreateTempDirectory() throws Exception
    // {
    // MuleClient muleClient = new MuleClient();
    //
    // dispatchAndWaitForDelivery(new DispatchParameters(INBOUND_ENDPOINT_NAME,
    // OUTBOUND_ENDPOINT_NAME));
    //
    // verifyInAndOutFiles(muleClient, INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME,
    // false, true);
    // }

}
