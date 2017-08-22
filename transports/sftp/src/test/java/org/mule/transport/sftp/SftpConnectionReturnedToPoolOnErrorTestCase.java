/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.sftp;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.util.concurrent.Latch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class SftpConnectionReturnedToPoolOnErrorTestCase extends AbstractSftpFunctionalTestCase
{

    public static final String FILE1_NAME = "file1.txt";

    private static AtomicInteger pollingInvokedTimes;
    private static String[] availableFiles;

    private static Latch sftpServerStopReq = new Latch();
    private static Latch sftpServerStopped = new Latch();

    @Override
    protected String getConfigFile()
    {
        return "mule-sftp-connection-returned-to-pool-on-error-config.xml";
    }

    @Override
    protected void setUpTestData() throws IOException
    {
        sftpClient.storeFile(FILE1_NAME, new ByteArrayInputStream(TEST_MESSAGE.getBytes()));
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        pollingInvokedTimes = new AtomicInteger(0);
        super.doSetUpBeforeMuleContextCreation();

        Executors.newSingleThreadExecutor().execute(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    sftpServerStopReq.await();
                    // Make sure that {@link SftpClient#disconnect()} doesn't throw an exception when the connection is
                    // already invalid, since that may interfere with how the pool works when an exception is thrown
                    // inside its destroy method.
                    // If this test fails because of an upgrade of the sftp library, the connection factory given to the
                    // pool has to account for this situation.
                    sftpServer.stop();
                    setUpServer();
                    sftpServerStopped.countDown();
                }
                catch (InterruptedException e)
                {
                    // Nothing to do
                }
            }
        });
    }

    @Test
    public void testConnectionReturned() throws Exception
    {
        SftpConnector c = (SftpConnector) muleContext.getRegistry().lookupConnector("sftpPooledConnector");
        final int connectorPoolSize = c.getMaxConnectionPoolSize();
        assertThat(c.useConnectionPool(), is(true));

        Prober prober = new PollingProber(RECEIVE_TIMEOUT, 50);
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return hasSftpServerStopped() && pollingInvokedTimes.get() > connectorPoolSize;
            }

            protected boolean hasSftpServerStopped()
            {
                return sftpServerStopReq.getCount() == 0;
            }

            @Override
            public String describeFailure()
            {
                return "The connection that was being used when the server went down was not returned to the pool.";
            }
        });
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

    public static class TestSftpReceiverRequesterUtil extends SftpReceiverRequesterUtil
    {

        public TestSftpReceiverRequesterUtil(ImmutableEndpoint endpoint)
        {
            super(endpoint);
        }

        @Override
        protected boolean canProcessFile(String fileName, SftpClient client, long fileAge, List<String> stableFiles) throws Exception
        {
            sftpServerStopReq.countDown();
            sftpServerStopped.await();
            return super.canProcessFile(fileName, client, fileAge, stableFiles);
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
                pollingInvokedTimes.incrementAndGet();
            }
        }

    }
}
