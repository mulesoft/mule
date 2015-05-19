/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.dataintegrity;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.client.MuleClient;
import org.mule.api.endpoint.ImmutableEndpoint;
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
public class SftpNoOutboundDirectoryTestCase extends AbstractSftpDataIntegrityTestCase
{
    private static final String ENDPOINT_NAME = "inboundEndpoint";

    public SftpNoOutboundDirectoryTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "dataintegrity/sftp-no-outbound-directory-config-service.xml"},
            {ConfigVariant.FLOW, "dataintegrity/sftp-no-outbound-directory-config-flow.xml"}});
    }

    @Override
    public void doSetUpBeforeMuleContextCreation() throws Exception
    {
        super.doSetUpBeforeMuleContextCreation();
        initEndpointDirectory(ENDPOINT_NAME);
    }

    /**
     * The outbound directory doesn't exist. The source file should still exist
     */
    @Test
    public void testNoOutboundDirectory() throws Exception
    {
        MuleClient muleClient = muleContext.getClient();

        // Send an file to the SFTP server, which the inbound-outboundEndpoint then
        // can pick up
        Exception exception = dispatchAndWaitForException(new DispatchParameters(ENDPOINT_NAME, null),
            "sftp", "service");
        assertNotNull(exception);

        assertTrue("expected DispatchException, but got " + exception.getClass().toString(),
            exception instanceof DispatchException);
        assertTrue("expected IOException, but got " + exception.getCause().getClass().toString(),
            exception.getCause() instanceof IOException);
        assertTrue("wrong starting message : " + exception.getCause().getMessage(), exception.getCause()
            .getMessage()
            .startsWith("Error 'No such file' occurred when trying to CDW to '"));
        assertTrue("wrong ending message : " + exception.getCause().getMessage(), exception.getCause()
            .getMessage()
            .endsWith("/DIRECTORY-MISSING'."));

        SftpClient sftpClient = getSftpClient(ENDPOINT_NAME);
        try
        {
            ImmutableEndpoint endpoint = muleContext.getRegistry().lookupObject(ENDPOINT_NAME);
            assertTrue("The inbound file should still exist",
                super.verifyFileExists(sftpClient, endpoint.getEndpointURI(), FILENAME));
        }
        finally
        {
            sftpClient.disconnect();
        }
    }

}
