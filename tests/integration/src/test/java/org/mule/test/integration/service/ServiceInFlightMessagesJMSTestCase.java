/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.service;

import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.JmsSupport;
import org.mule.util.queue.QueueSession;
import org.mule.util.xa.ResourceManagerSystemException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class ServiceInFlightMessagesJMSTestCase extends ServiceInFlightMessagesTestCase
{

    private final int timeout = getTimeoutSecs() * 1000 / 20;

    protected String getConfigResources()
    {
        return "org/mule/test/integration/service/service-inflight-messages-jms.xml";
    }

    /**
     * @assert Message are not lost when an un-paused seda service with
     *         non-persistent queue is stopped.
     * @assert All messages in non-persistent queue are processed before service is
     *         stopped.
     * @throws Exception
     */
    public void testInFlightMessages() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("TestService");
        TestJMSMessageListener listener = createTestJMSConsumer();
        populateSedaQueue(service, NUM_MESSAGES);

        muleContext.stop();

        assertTrue(listener.countdownLatch.await(timeout, TimeUnit.MILLISECONDS));

        assertNoLostMessages(NUM_MESSAGES, service, listener);
        // Seda queue is empty because queue is not persistent and therefore is
        // emptied when service is stopped
        assertSedaQueueEmpty(service);
    }

    /**
     * @assert Messages are lost when an paused seda service with non-persistent
     *         queue is stopped.
     * @throws Exception
     */
    public void testInFlightMessagesPausedService() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("PausedTestService");
        TestJMSMessageListener listener = createTestJMSConsumer();
        populateSedaQueue(service, NUM_MESSAGES);

        muleContext.stop();

        // All message were lost so both queues are empty.
        assertSedaQueueEmpty(service);
        // TODO Enable the following assertion once MULE-4072 is fixed
        // assertOutboundQueueEmpty(listener);
    }

    /**
     * @assert Messages are not lost when an un-paused seda service with persistent
     *         queue is stopped.
     * @assert NOT all messages in persistent queue are processed before service is
     *         stopped.
     * @throws Exception
     */
    public void testInFlightStopPersistentMessages() throws Exception
    {
        // TODO MULE-4253 (THIS SCENARIO FAILS INTERMITTENTLY)

        // Service service =
        // muleContext.getRegistry().lookupService("TestPersistentQueueService");
        // final TestJMSMessageListener listener = createTestJMSConsumer();
        // populateSedaQueue(service, NUM_MESSAGES);
        //
        // muleContext.stop();
        //
        // // Persistent queue is being used so seda queue is not emptied when
        // service
        // // is stopped
        // assertSedaQueueNotEmpty(service);
        //
        // // Start, process some messages, stop and make sure no messages get lost.
        // muleContext.start();
        // reregisterTestJMSConsumer(listener);
        //
        // assertTrue(listener.countdownLatch.await(timeout, TimeUnit.MILLISECONDS));
        // assertNoLostMessages(NUM_MESSAGES, service, listener);
        // assertSedaQueueEmpty(service);
        // // TODO Enable the following assertion once MULE-4072 is fixed
        // // assertOutboundQueueEmpty(listener);

    }

    /**
     * @assert Message are not lost when a paused seda service with persistent queue
     *         is stopped.
     * @assert NO messages are processed before service is stopped.
     * @assert When service is resumed all messages are processed.
     * @throws Exception
     */

    public void testInFlightStopPersistentMessagesPausedService() throws Exception
    {
        // TODO MULE-4253 (THIS SCENARIO FAILS INTERMITTENTLY)

        // Service service =
        // muleContext.getRegistry().lookupService("PausedTestPersistentQueueService"
        // );
        // TestJMSMessageListener listener = createTestJMSConsumer();
        // populateSedaQueue(service, NUM_MESSAGES);
        //
        // muleContext.stop();
        //
        // // Paused service does not process messages before or during stop().
        // assertNoLostMessages(NUM_MESSAGES, service, listener);
        //
        // // Start, process some messages, stop and make sure no messages get lost.
        // muleContext.start();
        // reregisterTestJMSConsumer(listener);
        // service.resume();
        //
        // listener.countdownLatch.await(LATCH_WAIT_TIME_MILLIS,
        // TimeUnit.MILLISECONDS);
        // assertNoLostMessages(NUM_MESSAGES, service, listener);
        // assertSedaQueueEmpty(service);
    }

    /**
     * @assert Messages are not lost when an seda service with persistent queue when
     *         mule is stopped and restarted as a new process
     * @assert NOT all messages in persistent queue are processed before service is
     *         stopped.
     * @assert Once mule is restarted messages in persistent queue are processed.
     * @throws Exception
     */
    public void testInFlightDisposePersistentMessages() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("TestPersistentQueueService");
        TestJMSMessageListener listener = createTestJMSConsumer();
        populateSedaQueue(service, NUM_MESSAGES);

        muleContext.stop();

        // Dispose and restart Mule and let it run for a short while
        muleContext.dispose();
        muleContext = createMuleContext();
        muleContext.start();
        reregisterTestJMSConsumer(listener);

        assertTrue(listener.countdownLatch.await(timeout, TimeUnit.MILLISECONDS));
        assertNoLostMessages(NUM_MESSAGES, service, listener);
        assertSedaQueueEmpty(service);
    }

    private TestJMSMessageListener createTestJMSConsumer() throws MuleException, JMSException
    {
        TestJMSMessageListener messageListener = new TestJMSMessageListener();
        createJMSMessageConsumer().setMessageListener(messageListener);
        return messageListener;
    }

    private void reregisterTestJMSConsumer(TestJMSMessageListener listener)
        throws MuleException, JMSException
    {
        createJMSMessageConsumer().setMessageListener(listener);
    }

    private MessageConsumer createJMSMessageConsumer() throws MuleException, JMSException
    {
        InboundEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "jms://out");
        JmsConnector jmsConnector = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector");
        JmsSupport jmsSupport = jmsConnector.getJmsSupport();
        MessageConsumer consumer = jmsSupport.createConsumer(jmsConnector.getSession(endpoint),
            jmsSupport.createDestination(jmsConnector.getSession(endpoint), endpoint), false);
        return consumer;
    }

    /**
     * After each run the following should total 500 events: 1) Event still in SEDA
     * queue 2) Events dispatched to outbound vm endpooint 3) Events that were unable
     * to be sent to stopped service and raised exceptions
     */
    private synchronized void assertNoLostMessages(int numMessages,
                                                   Service service,
                                                   TestJMSMessageListener listener)
        throws ResourceManagerSystemException
    {
        QueueSession queueSession = getTestQueueSession();
        logger.warn("SEDA Queue: " + queueSession.getQueue(service.getName() + ".service").size()
                    + ", Outbound JMS consumer: " + (NUM_MESSAGES - listener.countdownLatch.getCount()));
        assertEquals(numMessages, (NUM_MESSAGES - listener.countdownLatch.getCount())
                                  + queueSession.getQueue(service.getName() + ".service").size());
    }

    protected void assertOutboundQueueEmpty(TestJMSMessageListener listener)
    {
        assertEquals(500, listener.countdownLatch.getCount());
    }

    private class TestJMSMessageListener implements MessageListener
    {
        CountDownLatch countdownLatch = new CountDownLatch(ServiceInFlightMessagesJMSTestCase.NUM_MESSAGES);

        public void onMessage(Message message)
        {
            countdownLatch.countDown();
        }
    }

}
