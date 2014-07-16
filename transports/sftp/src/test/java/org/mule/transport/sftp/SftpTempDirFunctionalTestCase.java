/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class SftpTempDirFunctionalTestCase extends AbstractSftpTestCase
{
    private static final String OUTBOUND_ENDPOINT_NAME = "outboundEndpoint";
    private static final String INBOUND_ENDPOINT_NAME = "inboundEndpoint";
    private static final String INBOUND_ENDPOINT_NAME2 = "inboundEndpoint2";
    private static final String OUTBOUND_ENDPOINT_NAME2 = "outboundEndpoint2";
    private static final String TEMP_DIR = "uploading";

    public SftpTempDirFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "mule-sftp-temp-dir-config-service.xml"},
            {ConfigVariant.FLOW, "mule-sftp-temp-dir-config-flow.xml"}});
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        initEndpointDirectory(INBOUND_ENDPOINT_NAME);
        initEndpointDirectory(OUTBOUND_ENDPOINT_NAME);
        initEndpointDirectory(INBOUND_ENDPOINT_NAME2);
        initEndpointDirectory(OUTBOUND_ENDPOINT_NAME2);
    }

    @Test
    public void testTempDirInbound() throws Exception
    {
        DispatchParameters p = new DispatchParameters(INBOUND_ENDPOINT_NAME2, OUTBOUND_ENDPOINT_NAME2);
        p.setSftpConnector("sftpCustomConnectorTempDirInbound");
        dispatchAndWaitForDelivery(p);

        // Verify inbound
        SftpClient sftpClientInbound = getSftpClient(INBOUND_ENDPOINT_NAME2);
        ImmutableEndpoint endpointInbound = (ImmutableEndpoint) muleContext.getRegistry().lookupObject(INBOUND_ENDPOINT_NAME2);
        try
        {
            assertTrue("The temp directory should have been created",
                tempDirectoryExists(sftpClientInbound, INBOUND_ENDPOINT_NAME2));
            assertFalse(
                "No file should exist in the temp directory",
                super.verifyFileExists(sftpClientInbound, endpointInbound.getEndpointURI().getPath()
                                                          + "/uploading", FILENAME));
            assertFalse("The file should not exist in the source directory",
                super.verifyFileExists(sftpClientInbound, endpointInbound.getEndpointURI(), FILENAME));
        }
        finally
        {
            sftpClientInbound.disconnect();
        }

        // Verify outbound
        SftpClient sftpClientOutbound = getSftpClient(OUTBOUND_ENDPOINT_NAME2);
        ImmutableEndpoint endpointOutbound = muleContext.getRegistry().lookupObject(OUTBOUND_ENDPOINT_NAME2);
        try
        {
            assertFalse("The temp directory should not have been created",
                tempDirectoryExists(sftpClientOutbound, OUTBOUND_ENDPOINT_NAME2));
            assertTrue("The file should exist in the final destination : " + FILENAME,
                super.verifyFileExists(sftpClientOutbound, endpointOutbound.getEndpointURI(), FILENAME));
        }
        finally
        {
            sftpClientOutbound.disconnect();
        }

    }

    @Test
    public void testTempDirOutbound() throws Exception
    {
        DispatchParameters p = new DispatchParameters(INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME);
        p.setSftpConnector("sftpCustomConnector");
        dispatchAndWaitForDelivery(p);

        // Verify inbound
        SftpClient sftpClientInbound = getSftpClient(INBOUND_ENDPOINT_NAME);
        ImmutableEndpoint endpointInbound = muleContext.getRegistry().lookupObject(INBOUND_ENDPOINT_NAME);
        try
        {
            assertFalse("The temp directory should not have been created",
                tempDirectoryExists(sftpClientInbound, INBOUND_ENDPOINT_NAME));
            assertFalse("The file should not exist in the source directory",
                super.verifyFileExists(sftpClientInbound, endpointInbound.getEndpointURI(), FILENAME));
        }
        finally
        {
            sftpClientInbound.disconnect();
        }

        // Verify outbound
        SftpClient sftpClientOutbound = getSftpClient(OUTBOUND_ENDPOINT_NAME);
        ImmutableEndpoint endpointOutbound = muleContext.getRegistry().lookupObject(OUTBOUND_ENDPOINT_NAME);
        try
        {
            assertTrue("The temp directory should have been created",
                tempDirectoryExists(sftpClientOutbound, OUTBOUND_ENDPOINT_NAME));
            assertTrue("The file should exist in the final destination",
                super.verifyFileExists(sftpClientOutbound, endpointOutbound.getEndpointURI(), FILENAME));
        }
        finally
        {
            sftpClientOutbound.disconnect();
        }
    }

    private boolean tempDirectoryExists(SftpClient client, String endpointName)
        throws IOException
    {
        try
        {
            EndpointURI endpointURI = getUriByEndpointName(endpointName);

            client.changeWorkingDirectory(endpointURI.getPath() + "/" + TEMP_DIR);
            return true;
        }
        catch (IOException f)
        {
            return false;
        }
    }
}
