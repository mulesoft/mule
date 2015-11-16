/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.construct.Flow;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.store.QueuePersistenceObjectStore;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

public class ServiceInFlightMessagesTestCase extends FunctionalTestCase
{

    protected static final int WAIT_TIME_MILLIS = 500;
    protected static final int NUM_MESSAGES = 500;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/service/service-inflight-messages.xml";
    }

    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        // Use a graceful shutdown but not the full 5s default
        MuleContext context = super.createMuleContext();
        ((DefaultMuleConfiguration) context.getConfiguration()).setShutdownTimeout(WAIT_TIME_MILLIS);
        return context;
    }

    @Override
    protected void doTearDown() throws Exception
    {
        FileUtils.deleteDirectory(new File(muleContext.getConfiguration().getWorkingDirectory()));
        super.doTearDown();
    }

    @Test
    public void testInFlightMessagesWhenServiceStopped() throws Exception
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("TestService");
        populateSedaQueue(flow, NUM_MESSAGES);

        stopService(flow);

        assertNoLostMessages(NUM_MESSAGES, flow);
        // Seda queue is empty because queue is not persistent and therefore is
        // emptied when service is stopped
        assertSedaQueueEmpty(flow);
    }

    @Test
    @Ignore("MULE-6926: flaky test (caused by usage of Thead.sleep)")
    public void testInFlightMessagesPersistentQueueServiceWhenServiceStopped() throws Exception
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("TestPersistentQueueService");

        populateSedaQueue(flow, NUM_MESSAGES);

        stopService(flow);

        assertNoLostMessages(NUM_MESSAGES, flow);

        // Start, process some messages, stop and make sure no messages get lost.
        startService(flow);
        Thread.sleep(WAIT_TIME_MILLIS * 2);
        stopService(flow);

        assertNoLostMessages(NUM_MESSAGES, flow);

        // Let mule finish up with the rest of the messages until seda queue is empty
        startService(flow);
        Thread.sleep(WAIT_TIME_MILLIS * 10);
        stopService(flow);

        assertNoLostMessages(NUM_MESSAGES, flow);
        assertSedaQueueEmpty(flow);
    }

    @Test
    @Ignore("MULE-6926: flaky test (caused by usage of Thead.sleep)")
    public void testInFlightMessagesPersistentQueueServiceWhenMuleDisposed() throws Exception
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("TestPersistentQueueService");
        populateSedaQueue(flow, NUM_MESSAGES);

        muleContext.dispose();

        assertNoLostMessages(NUM_MESSAGES, flow);

        recreateAndStartMuleContext();
        Thread.sleep(WAIT_TIME_MILLIS);
        muleContext.dispose();

        assertNoLostMessages(NUM_MESSAGES, flow);

        // Let mule finish up with the rest of the messages until seda queue is empty
        recreateAndStartMuleContext();
        Thread.sleep(WAIT_TIME_MILLIS * 10);
        muleContext.dispose();

        assertNoLostMessages(NUM_MESSAGES, flow);
        assertSedaQueueEmpty(flow);
    }

    protected void recreateAndStartMuleContext() throws Exception, MuleException
    {
        muleContext = createMuleContext();
        muleContext.start();
    }

    protected void populateSedaQueue(Flow flow, int numMessages) throws MuleException, Exception
    {
        for (int i = 0; i < numMessages; i++)
        {
            flow.process((getTestEvent("test", getTestFlow(), muleContext.getEndpointFactory()
                    .getInboundEndpoint("test://test"))));
        }
    }

    /**
     * After each run the following should total 500 events: 1) Event still in SEDA
     * queue 2) Events dispatched to outbound vm endpooint 3) Events that were unable
     * to be sent to stopped service and raised exceptions
     *
     * @throws Exception
     */
    protected synchronized void assertNoLostMessages(int numMessages, Flow flow) throws Exception
    {
        logger.info("SEDA Queue: " + getSedaQueueSize(flow) + ", Outbound endpoint: "
                    + getOutSize());
        assertEquals(numMessages, getOutSize() + getSedaQueueSize(flow));
    }

    protected synchronized void assertSedaQueueEmpty(Flow flow) throws MuleException
    {
        assertEquals(0, getSedaQueueSize(flow));
    }

    protected synchronized void assertSedaQueueNotEmpty(Flow flow) throws MuleException
    {
        assertTrue(String.format("Seda queue for service '%s' is empty", flow.getName()),
                   getSedaQueueSize(flow) > 0);
    }

    protected synchronized void assertOutboundEmpty() throws Exception
    {
        assertEquals(0, getOutSize());
    }

    protected synchronized void assertOutboundNotEmpty() throws Exception
    {
        assertTrue("VM Out queue is empty", getOutSize() > 0);
    }

    protected int getSedaQueueSize(Flow flow) throws MuleException
    {
        return getQueueSize(getSedaQueueName(flow));
    }

    protected String getSedaQueueName(Flow flow)
    {
        return "seda.queue(" + flow.getName() + ")";
    }

    protected int getOutSize() throws Exception
    {
        return getQueueSize("out");
    }

    protected int getQueueSize(String name) throws MuleException
    {
        if (muleContext != null && muleContext.isStarted())
        {
            return muleContext.getQueueManager().getQueueSession().getQueue(name).size();
        }
        else
        {
            // Don;t fool around trying to use objects that weren't started fully, just go to the disk
            MuleContext localMuleContext = new DefaultMuleContextBuilder().buildMuleContext();
            String workingDirectory = localMuleContext.getConfiguration().getWorkingDirectory();
            String path = workingDirectory + File.separator + QueuePersistenceObjectStore.DEFAULT_QUEUE_STORE + File.separator + name;

            File[] filesInQueue = new File(path).listFiles();
            return filesInQueue.length;
        }
    }

    protected void stopService(Flow flow) throws Exception
    {
        flow.stop();
        muleContext.getRegistry().lookupConnector("outPersistentConnector").stop();
    }

    protected void startService(Flow flow) throws Exception
    {
        muleContext.getRegistry().lookupConnector("outPersistentConnector").start();
        flow.start();
    }

}
