/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.module.client.MuleClient;

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

    public SftpExpressionFilenameParserTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "mule-sftp-expressionFilenameParser-config.xml"},
            {ConfigVariant.FLOW, "mule-sftp-expressionFilenameParser-config.xml"}
        });
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
        MuleClient muleClient = new MuleClient(muleContext);
        dispatchAndWaitForDelivery(new DispatchParameters(INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME));

        SftpClient sftpClient = null;
        try
        {
            // Make sure a new file with name according to the notation has been
            // created
            sftpClient = getSftpClient(muleClient, OUTBOUND_ENDPOINT_NAME);
            ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(OUTBOUND_ENDPOINT_NAME);
            assertTrue("A new file in the outbound endpoint should exist", super.verifyFileExists(sftpClient,
                endpoint.getEndpointURI().getPath(), FILE_NAME));
        }
        finally
        {
            sftpClient.disconnect();
        }
    }
}
