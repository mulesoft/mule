/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.dataintegrity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transport.sftp.AbstractSftpTestCase;
import org.mule.transport.sftp.SftpClient;

import java.io.IOException;

public abstract class AbstractSftpDataIntegrityTestCase extends AbstractSftpTestCase
{
    protected static final String TEMP_DIR = "uploading";

    public AbstractSftpDataIntegrityTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    protected void verifyInAndOutFiles(String inboundEndpointName,
                                       String outboundEndpointName,
                                       boolean shouldInboundFileStillExist,
                                       boolean shouldOutboundFileExist) throws IOException
    {
        SftpClient sftpClientInbound = getSftpClient(inboundEndpointName);
        SftpClient sftpClientOutbound = getSftpClient(outboundEndpointName);

        try
        {
            ImmutableEndpoint inboundEndpoint = muleContext.getRegistry().lookupObject(inboundEndpointName);
            ImmutableEndpoint outboundEndpoint = muleContext.getRegistry().lookupObject(outboundEndpointName);

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
