
package org.mule.transport.sftp.dataintegrity;

import java.io.IOException;

import org.mule.api.transport.DispatchException;
import org.mule.module.client.MuleClient;
import org.mule.transport.sftp.SftpClient;

/**
 * Tests that files are not deleted if the temp directory can't be created
 */
public class SftpCantCreateTempDirectoryTestCase extends AbstractSftpDataIntegrityTestCase
{
    private static String INBOUND_ENDPOINT_NAME = "inboundEndpoint";
    private static String OUTBOUND_ENDPOINT_NAME = "outboundEndpoint";

    @Override
    protected String getConfigResources()
    {
        return "dataintegrity/sftp-dataintegrity-common-with-tempdir-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        // Delete the in & outbound directories
        initEndpointDirectory(INBOUND_ENDPOINT_NAME);
        initEndpointDirectory(OUTBOUND_ENDPOINT_NAME);
    }

    /**
     * No write access on the outbound directory and thus the TEMP directory cant be
     * created. The source file should still exist
     * 
     * @throws Exception If an error occurred
     */
    public void testCantCreateTempDirectory() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);

        SftpClient sftpClient = getSftpClient(muleClient, OUTBOUND_ENDPOINT_NAME);

        try
        {
            // change the chmod to "dr-x------" on the outbound-directory
            // --> the temp directory should not be able to be created
            remoteChmod(muleClient, sftpClient, OUTBOUND_ENDPOINT_NAME, 00500);

            // Send an file to the SFTP server, which the inbound-outboundEndpoint
            // then can pick up
            // Expect an error, permission denied
            Exception exception = dispatchAndWaitForException(new DispatchParameters(INBOUND_ENDPOINT_NAME,
                OUTBOUND_ENDPOINT_NAME), "sftp", "service");
            assertNotNull(exception);
            assertTrue("expected DispatchException, but got : " + exception.getClass().toString(),
                exception instanceof DispatchException);
            assertTrue("expected IOException, but got : " + exception.getCause().getClass().toString(),
                exception.getCause() instanceof IOException);
            assertEquals("Could not create the directory 'uploading', caused by: Permission denied",
                exception.getCause().getMessage());

            verifyInAndOutFiles(muleClient, INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME, true, false);
        }
        finally
        {
            sftpClient.disconnect();
        }
    }

    /**
     * The same test as above, but with the difference that this time it should be
     * okay to create the directory, and the file should be gone from the inbound
     * directory.
     */
    // Works, but this is more or less the same test as SftpTempDirFunctionalTestCase
    // so don't use this
    // public void testCanCreateTempDirectory() throws Exception
    // {
    // MuleClient muleClient = new MuleClient();
    //
    // dispatchAndWaitForDelivery(new DispatchParameters(INBOUND_ENDPOINT_NAME,
    // OUTBOUND_ENDPOINT_NAME));
    //	
    // verifyInAndOutFiles(muleClient, INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME,
    // false, true);
    // }

}
