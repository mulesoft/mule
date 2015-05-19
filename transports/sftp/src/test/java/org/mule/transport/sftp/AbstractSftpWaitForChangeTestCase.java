/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.sftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public abstract class AbstractSftpWaitForChangeTestCase extends AbstractSftpFunctionalTestCase
{

    public static final String FILE1_NAME = "file1.txt";
    public static final String FILE2_NAME = "file2.txt";

    private static boolean pollingInvoked;
    private static String[] availableFiles;


    @Override
    protected void setUpTestData() throws IOException
    {
        sftpClient.storeFile(FILE1_NAME, new ByteArrayInputStream(TEST_MESSAGE.getBytes()));
        sftpClient.storeFile(FILE2_NAME, new ByteArrayInputStream(TEST_MESSAGE.getBytes()));
    }

    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        pollingInvoked = false;
        availableFiles = null;
        super.doSetUpBeforeMuleContextCreation();
    }

    @Test
    public void detectsDeletedFilesWhileWaitingForFileChanges() throws Exception
    {

        Prober prober = new PollingProber(RECEIVE_TIMEOUT, 50);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                return pollingInvoked;
            }

            public String describeFailure()
            {
                return "SFTP poll was not invoked";
            }
        });

        assertNotNull("Polling did not return any result", availableFiles);
        assertEquals(1, availableFiles.length);
        assertEquals(FILE2_NAME, availableFiles[0]);
    }

    public static class TestSftpMessageReceiver extends SftpMessageReceiver
    {

        public TestSftpMessageReceiver(SftpConnector connector, FlowConstruct flow, InboundEndpoint endpoint, long frequency) throws CreateException
        {
            super(connector, flow, endpoint, frequency);
        }

        @Override
        protected SftpReceiverRequesterUtil createSftpReceiverRequesterUtil(InboundEndpoint endpoint)
        {
            return new TestSftpReceiverRequesterUtil(endpoint);
        }
    }

    private static void deleteSftpFile(String fileName)
    {
        try
        {
            SftpClient sftpClient = createDefaultSftpClient(Integer.parseInt(System.getProperty(SFTP_PORT)));

            sftpClient.changeWorkingDirectory(TESTDIR);
            sftpClient.deleteFile(fileName);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error trying to delete test file");
        }
    }

    public static class TestSftpReceiverRequesterUtil extends SftpReceiverRequesterUtil
    {

        public TestSftpReceiverRequesterUtil(ImmutableEndpoint endpoint)
        {
            super(endpoint);
        }

        @Override
        protected boolean canProcessFile(String fileName, SftpClient client, long fileAge, long sizeCheckDelayMs) throws Exception
        {
            if (FILE1_NAME.equals(fileName))
            {
                deleteSftpFile(fileName);
            }

            return super.canProcessFile(fileName, client, fileAge, sizeCheckDelayMs);
        }

        @Override
        public String[] getAvailableFiles(boolean onlyGetTheFirstOne) throws Exception
        {
            try
            {
                availableFiles = super.getAvailableFiles(onlyGetTheFirstOne);
                return availableFiles;
            }
            finally
            {
                pollingInvoked = true;
            }
        }

    }
}
