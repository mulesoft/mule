/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertTrue;
import org.mule.api.endpoint.ImmutableEndpoint;

import org.junit.Test;

/**
 * @author Magnus Larsson <code>SftpExpressionFilenameParserTestCase</code> tests
 *         usage of the Expression Filename Parser instead of the default Legacy
 *         Parser.
 */
public class SftpExpressionFilenameParserTestCase extends AbstractSftpTestCase
{
    protected static final long TIMEOUT = 10000;
    private static final String OUTBOUND_ENDPOINT_NAME = "outboundEndpoint";
    private static final String INBOUND_ENDPOINT_NAME = "inboundEndpoint";

    @Override
    protected String getConfigFile()
    {
        return "mule-sftp-expressionFilenameParser-config-flow.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        initEndpointDirectory(INBOUND_ENDPOINT_NAME);
        initEndpointDirectory(OUTBOUND_ENDPOINT_NAME);
    }

    @Test
    public void testExpressionFilenameParser() throws Exception
    {
        dispatchAndWaitForDelivery(new DispatchParameters(INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME));

        SftpClient client = null;
        try
        {
            // Make sure a new file with name according to the notation has been
            // created
            client = getSftpClient(OUTBOUND_ENDPOINT_NAME);
            ImmutableEndpoint endpoint = muleContext.getRegistry().lookupObject(OUTBOUND_ENDPOINT_NAME);
            assertTrue("A new file in the outbound endpoint should exist", super.verifyFileExists(client,
                endpoint.getEndpointURI().getPath(), FILENAME));
        }
        finally
        {
            if (client != null)
            {
                client.disconnect();
            }
        }
    }
}
