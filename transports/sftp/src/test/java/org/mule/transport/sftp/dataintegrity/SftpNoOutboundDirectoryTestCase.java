/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.sftp.dataintegrity;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.transport.sftp.SftpClient;

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
    public void before() throws Exception
    {
        super.before();
        initEndpointDirectory(ENDPOINT_NAME);
    }

    /**
     * The outbound directory doesn't exist. The source file should still exist
     */
    @Test
    public void testNoOutboundDirectory() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);

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

        SftpClient sftpClient = getSftpClient(muleClient, ENDPOINT_NAME);
        try
        {
            ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(ENDPOINT_NAME);
            assertTrue("The inbound file should still exist",
                super.verifyFileExists(sftpClient, endpoint.getEndpointURI(), FILENAME));
        }
        finally
        {
            sftpClient.disconnect();
        }
    }

}
