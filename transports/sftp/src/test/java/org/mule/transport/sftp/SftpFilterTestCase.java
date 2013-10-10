/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.transport.sftp.dataintegrity.AbstractSftpDataIntegrityTestCase;

/**
 * Simple test to verify that the filter configuration works. Note that the transport
 * uses an "early" filter to improves the performance.
 */
public class SftpFilterTestCase extends AbstractSftpDataIntegrityTestCase
{
    private static String INBOUND_ENDPOINT_NAME = "inboundEndpoint";
    private static String OUTBOUND_ENDPOINT_NAME = "outboundEndpoint";

    public SftpFilterTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "mule-sftp-filter-config-service.xml"},
            {ConfigVariant.FLOW, "mule-sftp-filter-config-flow.xml"}
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
    public void testFilter() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);

        // Send .txt file using muleclient.dipatch directly (since the file won't be
        // delivered to the endpoint (due to filter settings) we can't wait for a
        // delivery notification....
        HashMap<String, String> txtProps = new HashMap<String, String>(1);
        txtProps.put(SftpConnector.PROPERTY_FILENAME, FILENAME);
        muleClient.dispatch(getAddressByEndpoint(muleClient, INBOUND_ENDPOINT_NAME), TEST_MESSAGE, txtProps);

        // Send .xml file
        DispatchParameters dp = new DispatchParameters(INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME);
        dp.setFilename("file.xml");
        dispatchAndWaitForDelivery(dp);

        SftpClient outboundSftpClient = getSftpClient(muleClient, OUTBOUND_ENDPOINT_NAME);
        ImmutableEndpoint outboundEndpoint = (ImmutableEndpoint) muleClient.getProperty(OUTBOUND_ENDPOINT_NAME);

        SftpClient inboundSftpClient = getSftpClient(muleClient, INBOUND_ENDPOINT_NAME);
        ImmutableEndpoint inboundEndpoint = (ImmutableEndpoint) muleClient.getProperty(INBOUND_ENDPOINT_NAME);

        assertFalse("The xml file should not be left in the inbound directory", verifyFileExists(
            inboundSftpClient, inboundEndpoint.getEndpointURI().getPath(), "file.xml"));
        assertTrue("The xml file should be in the outbound directory", verifyFileExists(outboundSftpClient,
            outboundEndpoint.getEndpointURI().getPath(), "file.xml"));

        assertTrue("The txt file should be left in the inbound directory", verifyFileExists(
            inboundSftpClient, inboundEndpoint.getEndpointURI().getPath(), FILENAME));
        assertFalse("The txt file should not be in the outbound directory", verifyFileExists(
            outboundSftpClient, outboundEndpoint.getEndpointURI().getPath(), FILENAME));
    }
}
