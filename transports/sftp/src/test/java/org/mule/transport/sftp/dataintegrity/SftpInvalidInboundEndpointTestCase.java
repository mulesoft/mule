
package org.mule.transport.sftp.dataintegrity;

import java.io.IOException;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.registry.MuleRegistry;
import org.mule.transport.sftp.SftpConnector;

public class SftpInvalidInboundEndpointTestCase extends AbstractSftpDataIntegrityTestCase
{

    private static final int NO_OF_INVALID_ATTEMPTS = 50;

    @Override
    protected String getConfigResources()
    {
        return "dataintegrity/sftp-invalid-inbound-endpoint-config.xml";
    }

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
