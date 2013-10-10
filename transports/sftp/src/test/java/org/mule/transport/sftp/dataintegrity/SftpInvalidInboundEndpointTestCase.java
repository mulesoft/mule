/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.sftp.dataintegrity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.registry.MuleRegistry;
import org.mule.transport.sftp.SftpConnector;

public class SftpInvalidInboundEndpointTestCase extends AbstractSftpDataIntegrityTestCase
{
    private static final int NO_OF_INVALID_ATTEMPTS = 50;

    public SftpInvalidInboundEndpointTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE,
            "dataintegrity/sftp-invalid-inbound-endpoint-config.xml"}});
    }

    @Test
    public void testInvalidInboundEndpoint() throws Exception
    {

        String expectedStartOfErrorMessage = "Error 'No such file' occurred when trying to CDW";

        MuleRegistry registry = muleContext.getRegistry();

        SftpConnector c = (SftpConnector) registry.lookupConnector("sftp");
        assertNotNull(c);

        EndpointBuilder epb = registry.lookupEndpointBuilder("InvalidEndpoint");
        InboundEndpoint ep = epb.buildInboundEndpoint();

        // Verify that failed creations of sftp-clients don't leak resources (e.g.
        // ssh-servers)
        // In v2.2.1-RC2 this tests fails after 132 attempts on a Mac OSX 10.6
        // machine
        for (int i = 0; i < NO_OF_INVALID_ATTEMPTS; i++)
        {
            if (logger.isDebugEnabled())
                logger.debug("CreateSftpClient invalid atempt #" + i + " of " + NO_OF_INVALID_ATTEMPTS);
            try
            {
                c.createSftpClient(ep);
                fail("Should have received an exception here!!!");
            }
            catch (IOException ioe)
            {
                String actualStartOfErrorMessage = ioe.getMessage().substring(0,
                    expectedStartOfErrorMessage.length());
                assertEquals(expectedStartOfErrorMessage, actualStartOfErrorMessage);
            }
        }
    }
}
