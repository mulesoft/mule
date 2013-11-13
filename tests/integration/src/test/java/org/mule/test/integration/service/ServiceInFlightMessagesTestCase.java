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
import org.mule.api.service.Service;
import org.mule.config.DefaultMuleConfiguration;
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
        Service service = muleContext.getRegistry().lookupService("TestService");
        populateSedaQueue(service, NUM_MESSAGES);

        stopService(service);

        assertNoLostMessages(NUM_MESSAGES, service);
        // Seda queue is empty because queue is not persistent and therefore is
        // emptied when service is stopped
        assertSedaQueueEmpty(service);
    }

    @Test
    public void testInFlightMessagesPausedServiceWhenServiceStopped() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("PausedTestService");
        populateSedaQueue(service, NUM_MESSAGES);

        stopService(service);

        // The service is paused so no message get processed. Because the service is
        // stopped messages aren't lost. If Mule was disposed then messages would be
        // lost
        assertNoLostMessages(NUM_MESSAGES, service);

        assertOutboundEmpty();
    }

    @Test
    @Ignore("MULE-6926: flaky test (caused by usage of Thead.sleep)")
    public void testInFlightMessagesPersistentQueueServiceWhenServiceStopped() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("TestPersistentQueueService");

        populateSedaQueue(service, NUM_MESSAGES);

        stopService(service);

        assertNoLostMessages(NUM_MESSAGES, service);

        // Start, process some messages, stop and make sure no messages get lost.
        startService(service);
        Thread.sleep(WAIT_TIME_MILLIS * 2);
        stopService(service);

        assertNoLostMessages(NUM_MESSAGES, service);

        // Let mule finish up with the rest of the messages until seda queue is empty
        startService(service);
        Thread.sleep(WAIT_TIME_MILLIS * 10);
        stopService(service);

        assertNoLostMessages(NUM_MESSAGES, service);
        assertSedaQueueEmpty(service);
    }

    @Test
    @Ignore("MULE-6926: flaky test (caused by usage of Thead.sleep)")
    public void testInFlightMessagesPausedPersistentQueueServiceWhenServiceStopped() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("PausedTestPersistentQueueService");
        populateSedaQueue(service, NUM_MESSAGES);

        stopService(service);

        // Paused service does not process messages before or during stop().
        assertOutboundEmpty();
        assertNoLostMessages(NUM_MESSAGES, service);

        // Start, process some messages, stop and make sure no messages get lost.
        startService(service);
        service.resume();
        Thread.sleep(WAIT_TIME_MILLIS * 2);
        stopService(service);

        assertNoLostMessages(NUM_MESSAGES, service);

        // Let mule finish up with the rest of the messages until seda queue is empty
        startService(service);
        service.resume();
        Thread.sleep(WAIT_TIME_MILLIS * 20);
        stopService(service);

        assertNoLostMessages(NUM_MESSAGES, service);
        assertSedaQueueEmpty(service);
    }

    @Test
    @Ignore("MULE-6926: flaky test (caused by usage of Thead.sleep)")
    public void testInFlightMessagesPersistentQueueServiceWhenMuleDisposed() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("TestPersistentQueueService");
        populateSedaQueue(service, NUM_MESSAGES);

        muleContext.dispose();

        assertNoLostMessages(NUM_MESSAGES, service);

        recreateAndStartMuleContext();
        Thread.sleep(WAIT_TIME_MILLIS);
        muleContext.dispose();

        assertNoLostMessages(NUM_MESSAGES, service);

        // Let mule finish up with the rest of the messages until seda queue is empty
        recreateAndStartMuleContext();
        Thread.sleep(WAIT_TIME_MILLIS * 10);
        muleContext.dispose();

        assertNoLostMessages(NUM_MESSAGES, service);
        assertSedaQueueEmpty(service);
    }

    protected void recreateAndStartMuleContext() throws Exception, MuleException
    {
        muleContext = createMuleContext();
        muleContext.start();
    }

    protected void populateSedaQueue(Service service, int numMessages) throws MuleException, Exception
    {
        for (int i = 0; i < numMessages; i++)
        {
            service.dispatchEvent(getTestEvent("test", service, muleContext.getEndpointFactory()
                .getInboundEndpoint("test://test")));
        }
    }

    /**
     * After each run the following should total 500 events: 1) Event still in SEDA
     * queue 2) Events dispatched to outbound vm endpooint 3) Events that were unable
     * to be sent to stopped service and raised exceptions
     *
     * @throws Exception
     */
    protected synchronized void assertNoLostMessages(int numMessages, Service service) throws Exception
    {
        logger.info("SEDA Queue: " + getSedaQueueSize(service) + ", Outbound endpoint: "
                    + getOutSize());
        assertEquals(numMessages, getOutSize() + getSedaQueueSize(service));
    }

    protected synchronized void assertSedaQueueEmpty(Service service) throws MuleException
    {
        assertEquals(0, getSedaQueueSize(service));
    }

    protected synchronized void assertSedaQueueNotEmpty(Service service) throws MuleException
    {
        assertTrue(String.format("Seda queue for service '%s' is empty", service.getName()),
            getSedaQueueSize(service) > 0);
    }

    protected synchronized void assertOutboundEmpty() throws Exception
    {
        assertEquals(0, getOutSize());
    }

    protected synchronized void assertOutboundNotEmpty() throws Exception
    {
        assertTrue("VM Out queue is empty", getOutSize() > 0);
    }

    protected int getSedaQueueSize(Service service) throws MuleException
    {
        return getQueueSize(getSedaQueueName(service));
    }

    protected String getSedaQueueName(Service service)
    {
        return "seda.queue(" + service.getName() + ")";
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

    protected void stopService(Service service) throws Exception
    {
        service.stop();
        muleContext.getRegistry().lookupConnector("outPersistentConnector").stop();
    }

    protected void startService(Service service) throws Exception
    {
        muleContext.getRegistry().lookupConnector("outPersistentConnector").start();
        service.start();
    }

}
