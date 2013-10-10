/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.sftp.dataintegrity;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.transport.sftp.AbstractSftpTestCase;
import org.mule.transport.sftp.SftpClient;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class AbstractSftpDataIntegrityTestCase extends AbstractSftpTestCase
{
    protected static final String TEMP_DIR = "uploading";

    public AbstractSftpDataIntegrityTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    protected void verifyInAndOutFiles(MuleClient muleClient,
                                       String inboundEndpointName,
                                       String outboundEndpointName,
                                       boolean shouldInboundFileStillExist,
                                       boolean shouldOutboundFileExist) throws IOException
    {
        SftpClient sftpClientInbound = getSftpClient(muleClient, inboundEndpointName);
        SftpClient sftpClientOutbound = getSftpClient(muleClient, outboundEndpointName);

        try
        {
            ImmutableEndpoint inboundEndpoint = (ImmutableEndpoint) muleClient.getProperty(inboundEndpointName);

            ImmutableEndpoint outboundEndpoint = (ImmutableEndpoint) muleClient.getProperty(outboundEndpointName);

            if (shouldInboundFileStillExist)
            {
                assertTrue("The inbound file should still exist",
                    super.verifyFileExists(sftpClientInbound, inboundEndpoint.getEndpointURI(), FILENAME));
            }
            else
            {
                assertFalse("The inbound file should have been deleted",
                    super.verifyFileExists(sftpClientInbound, inboundEndpoint.getEndpointURI(), FILENAME));
            }

            if (shouldOutboundFileExist)
            {
                assertTrue("The outbound file should exist",
                    super.verifyFileExists(sftpClientOutbound, outboundEndpoint.getEndpointURI(), FILENAME));
            }
            else
            {
                assertFalse("The outbound file should have been deleted",
                    super.verifyFileExists(sftpClientOutbound, outboundEndpoint.getEndpointURI(), FILENAME));
            }
        }
        finally
        {
            sftpClientInbound.disconnect();
            sftpClientOutbound.disconnect();
        }
    }

}
