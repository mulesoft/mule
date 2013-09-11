/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.dataintegrity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.transport.DispatchException;
import org.mule.transport.sftp.SftpClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Verify that the original file is not lost if the outbound directory doesn't exist
 */
public class SftpNoWriteAccessToOutboundDirectoryTestCase extends AbstractSftpDataIntegrityTestCase
{
    private static final String OUTBOUND_ENDPOINT_NAME = "outboundEndpoint";
    private static final String INBOUND_ENDPOINT_NAME = "inboundEndpoint";

    public SftpNoWriteAccessToOutboundDirectoryTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "dataintegrity/sftp-dataintegrity-common-config-service.xml"},
            {ConfigVariant.FLOW, "dataintegrity/sftp-dataintegrity-common-config-flow.xml"}});
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        // Delete the in & outbound directories
        initEndpointDirectory(INBOUND_ENDPOINT_NAME);
        initEndpointDirectory(OUTBOUND_ENDPOINT_NAME);
    }

    /** No write access on the outbound directory. The source file should still exist */
    @Test
    public void testNoWriteAccessToOutboundDirectory() throws Exception
    {
        SftpClient client = getSftpClient(OUTBOUND_ENDPOINT_NAME);

        try
        {
            // change the chmod to "dr-x------" on the outbound-directory
            remoteChmod(client, OUTBOUND_ENDPOINT_NAME, 00500);

            // Send an file to the SFTP server, which the inbound-outboundEndpoint
            // then can pick up
            Exception exception = dispatchAndWaitForException(new DispatchParameters(INBOUND_ENDPOINT_NAME,
                OUTBOUND_ENDPOINT_NAME), "sftp", "service");
            assertNotNull(exception);
            assertTrue("expected DispatchException, but got : " + exception.getClass().toString(),
                exception instanceof DispatchException);
            assertTrue("expected IOException, but got : " + exception.getCause().getClass().toString(),
                exception.getCause() instanceof IOException);
            assertEquals("Permission denied", exception.getCause().getMessage());

            verifyInAndOutFiles(INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME, true, false);
        }
        finally
        {
            client.disconnect();
        }
    }
}
