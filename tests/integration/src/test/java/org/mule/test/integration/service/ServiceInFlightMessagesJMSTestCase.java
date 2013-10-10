/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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

import java.util.concurrent.CountDownLatch;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

public class ServiceInFlightMessagesJMSTestCase extends ServiceInFlightMessagesTestCase
{
    protected TestJMSMessageListener listener;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/service/service-inflight-messages-jms.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        listener = createTestJMSConsumer();
    }

    protected void stopService(Service service) throws Exception
    {
        service.stop();
        // Give connector and jms broker some time to process all pending messages
        Thread.sleep(WAIT_TIME_MILLIS);
    }

    protected void startService(Service service) throws Exception
    {
        service.start();
    }

    private TestJMSMessageListener createTestJMSConsumer() throws MuleException, JMSException
    {
        TestJMSMessageListener messageListener = new TestJMSMessageListener();
        createJMSMessageConsumer().setMessageListener(messageListener);
        return messageListener;
    }

    private MessageConsumer createJMSMessageConsumer() throws MuleException, JMSException
    {
        InboundEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("jms://out");
        JmsConnector jmsConnector = (JmsConnector) muleContext.getRegistry().lookupConnector(
            "outPersistentConnector");
        JmsSupport jmsSupport = jmsConnector.getJmsSupport();
        MessageConsumer consumer = jmsSupport.createConsumer(jmsConnector.getSession(endpoint),
            jmsSupport.createDestination(jmsConnector.getSession(endpoint), endpoint), false, endpoint);
        return consumer;
    }

    protected int getOutSize() throws Exception
    {
        return (int) (500 - listener.countdownLatch.getCount());
    }

    protected void recreateAndStartMuleContext() throws Exception, MuleException
    {
        muleContext = createMuleContext();
        muleContext.start();
        createJMSMessageConsumer().setMessageListener(listener);
    }

    private class TestJMSMessageListener implements MessageListener
    {
        public TestJMSMessageListener()
        {
            super();
        }

        CountDownLatch countdownLatch = new CountDownLatch(ServiceInFlightMessagesJMSTestCase.NUM_MESSAGES);

        public void onMessage(Message message)
        {
            countdownLatch.countDown();
        }
    }
}
