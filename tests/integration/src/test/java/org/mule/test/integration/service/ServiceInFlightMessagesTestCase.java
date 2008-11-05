/*
 * $Id: EndpointBridgingTestCase.java 10662 2008-02-01 13:10:14Z romikk $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.service;

import org.mule.api.service.Service;
import org.mule.tck.FunctionalTestCase;
import org.mule.util.queue.FilePersistenceStrategy;
import org.mule.util.queue.QueueSession;
import org.mule.util.queue.TransactionalQueueManager;
import org.mule.util.xa.ResourceManagerSystemException;

public class ServiceInFlightMessagesTestCase extends FunctionalTestCase
{

    private static final int WAIT_TIME_MILLIS = 0;
    private static final int NUM_MESSAGES = 50;

    protected String getConfigResources()
    {
        return "org/mule/test/integration/service/service-inflight-messages.xml";
    }

    public void testInFlightMessages() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("TestService");
        int numMessage = NUM_MESSAGES;
        for (int i = 0; i < numMessage; i++)
        {
            service.dispatchEvent(getTestEvent("test", service, getTestInboundEndpoint("test://test")));
        }

        // Stop rather than dispose so we still have access to the connector for this
        // test.
        muleContext.stop();

        assertQueues(numMessage, "TestService");
    }

    public void testInFlightMessagesPausedService() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("PausedTestService");
        int numMessage = NUM_MESSAGES;
        for (int i = 0; i < numMessage; i++)
        {
            service.dispatchEvent(getTestEvent("test", service, getTestInboundEndpoint("test://test")));
        }

        Thread.sleep(100);

        // Stop rather than dispose so we still have access to the connector for this
        // test.
        muleContext.stop();

        // Messages are lost when paused service is stopped
        assertEquals(0, getTestQueueSession().getQueue("out").size());
        assertEquals(0, getTestQueueSession().getQueue("PausedTestService.service").size());
    }

    public void testInFlightStopPersistentMessages() throws Exception
    {

        Service service = muleContext.getRegistry().lookupService("TestPersistentQueueService");
        int numMessage = NUM_MESSAGES;
        for (int i = 0; i < numMessage; i++)
        {
            service.dispatchEvent(getTestEvent("test", service, getTestInboundEndpoint("test://test")));
        }

        // Stop service and give workers a chance to finnish up before insepcting
        // queues
        muleContext.stop();
        Thread.sleep(WAIT_TIME_MILLIS);

        assertQueues(numMessage, "TestPersistentQueueService");

        muleContext.start();
        Thread.sleep(WAIT_TIME_MILLIS);
        muleContext.stop();
        Thread.sleep(WAIT_TIME_MILLIS);

        assertQueues(numMessage, "TestPersistentQueueService");

        // Let mule finnish up with the rest of the messages until seda queue is
        // // empty
        muleContext.start();
        Thread.sleep(1000);
        muleContext.stop();

        assertQueues(numMessage, "TestPersistentQueueService");

    }

    public void testInFlightStopPersistentMessagesPausedService() throws Exception
    {

        Service service = muleContext.getRegistry().lookupService("PausedTestPersistentQueueService");
        int numMessage = NUM_MESSAGES;
        for (int i = 0; i < numMessage; i++)
        {
            service.dispatchEvent(getTestEvent("test", service, getTestInboundEndpoint("test://test")));
        }

        // Stop service and give workers a chance to finnish up before insepcting
        // queues
        muleContext.stop();
        Thread.sleep(WAIT_TIME_MILLIS);

        // Paused service does not process messages before or during stop().
        assertEquals(0, getTestQueueSession().getQueue("out").size());
        assertQueues(numMessage, "PausedTestPersistentQueueService");

        muleContext.start();
        Thread.sleep(WAIT_TIME_MILLIS);
        muleContext.stop();
        Thread.sleep(WAIT_TIME_MILLIS);

        // Paused service process messages before or during stop().
        assertTrue(getTestQueueSession().getQueue("out").size() > 1);
        assertQueues(numMessage, "PausedTestPersistentQueueService");

        service.resume();
        Thread.sleep(100);

        // Paused service processes messages when resumed.
        assertTrue(getTestQueueSession().getQueue("out").size() > 0);
        assertQueues(numMessage, "PausedTestPersistentQueueService");

        // Let mule finnish up with the rest of the messages until seda queue is
        // // empty
        muleContext.start();
        Thread.sleep(1000);
        muleContext.stop();

        assertQueues(numMessage, "PausedTestPersistentQueueService");

    }

    public void testInFlightDisposePersistentMessages() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("TestPersistentQueueService");
        int numMessage = NUM_MESSAGES;
        for (int i = 0; i < numMessage; i++)
        {
            service.dispatchEvent(getTestEvent("test", service, getTestInboundEndpoint("test://test")));
        }

        // 2) Stop service and give it's workers a chance to finish executing before
        // inspecting
        // queues.
        muleContext.stop();
        Thread.sleep(WAIT_TIME_MILLIS);
        assertQueues(numMessage, "TestPersistentQueueService");

        // 3) Dispose and restart Mule and let it run for a short while
        muleContext.dispose();
        muleContext = createMuleContext();
        muleContext.start();
        Thread.sleep(WAIT_TIME_MILLIS);

        // Stop service and give workers a chance to finnish up before insepcting
        // queues
        muleContext.stop();
        Thread.sleep(WAIT_TIME_MILLIS);

        assertQueues(numMessage, "TestPersistentQueueService");

        // Let mule finnish up with the rest of the messages until seda queue is
        // empty
        muleContext.start();
        Thread.sleep(1000);
        muleContext.stop();
        assertQueues(numMessage, "TestPersistentQueueService");
    }

    /**
     * After each run the following should totoal 500 events: 1) Event still in SEDA
     * queue 2) Events dispatched to outbound vm endpooint 3) Events that were unable
     * to be sent to stopped service and raised exceptions
     */
    private synchronized void assertQueues(int numMessage, String service) throws ResourceManagerSystemException
    {
        QueueSession queueSession = getTestQueueSession();
        logger.info("SEDA Queue: " + queueSession.getQueue("out").size() + ", Outbound endpoint vm queue: "
                    + queueSession.getQueue(service + ".service").size());
        assertEquals(numMessage, queueSession.getQueue("out").size()
                                 + queueSession.getQueue(service + ".service").size());
    }

    private QueueSession getTestQueueSession() throws ResourceManagerSystemException
    {
        TransactionalQueueManager tqm = new TransactionalQueueManager();
        FilePersistenceStrategy fps = new FilePersistenceStrategy();
        fps.setMuleContext(muleContext);
        tqm.setPersistenceStrategy(fps);
        tqm.start();
        QueueSession queueSession = tqm.getQueueSession();
        return queueSession;
    }

}
