
package org.mule.transport.sftp.dataintegrity;

import java.io.IOException;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.transport.sftp.AbstractSftpTestCase;
import org.mule.transport.sftp.SftpClient;

/**
 *
 */
public abstract class AbstractSftpDataIntegrityTestCase extends AbstractSftpTestCase
{

    protected static final String TEMP_DIR = "uploading";

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
                assertTrue("The inbound file should still exist", super.verifyFileExists(sftpClientInbound,
                    inboundEndpoint.getEndpointURI(), FILE_NAME));
            }
            else
            {
                assertFalse("The inbound file should have been deleted", super.verifyFileExists(
                    sftpClientInbound, inboundEndpoint.getEndpointURI(), FILE_NAME));
            }

            if (shouldOutboundFileExist)
            {
                assertTrue("The outbound file should exist", super.verifyFileExists(sftpClientOutbound,
                    outboundEndpoint.getEndpointURI(), FILE_NAME));
            }
            else
            {
                assertFalse("The outbound file should have been deleted", super.verifyFileExists(
                    sftpClientOutbound, outboundEndpoint.getEndpointURI(), FILE_NAME));
            }
        }
        finally
        {
            sftpClientInbound.disconnect();
            sftpClientOutbound.disconnect();
        }
    }

}
