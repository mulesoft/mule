/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.functional.junit4.FunctionalTestCase;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Test;

public class QosHeadersTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/providers/jms/qosheaders-test-flow.xml";
    }

    @Test
    public void testQosHeadersHonored() throws JMSException
    {
        String producerQueue = "test.in.kind";
        String consumerQueue = "test.out.kind";
        doSendReceiveCycle(producerQueue, consumerQueue, true);
    }

    @Test
    public void testQosHeadersNotHonored() throws JMSException
    {
        String producerQueue = "test.in.selfish";
        String consumerQueue = "test.out.selfish";
        doSendReceiveCycle(producerQueue, consumerQueue, false);
    }

    /**
     * @param honorProperties indicate which assertion path to take
     */
    protected void doSendReceiveCycle(final String producerQueue,
                                      final String consumerQueue,
                                      final boolean honorProperties) throws JMSException
    {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
            "vm://localhost?broker.persistent=false&broker.useJmx=false");
        Connection producerConnection = null;
        Connection consumerConnection = null;

        try
        {
            // Producer part
            producerConnection = connectionFactory.createConnection();
            producerConnection.start();

            Session producerSession = producerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination producerDestination = producerSession.createQueue(producerQueue);
            MessageProducer producer = producerSession.createProducer(producerDestination);

            // Consumer part
            consumerConnection = connectionFactory.createConnection();
            consumerConnection.start();

            Session consumerSession = consumerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination consumerDestination = consumerSession.createQueue(consumerQueue);
            MessageConsumer consumer = consumerSession.createConsumer(consumerDestination);

            String message = "QoS Headers Propagation Test";
            TextMessage textMessage = producerSession.createTextMessage(message);
            producer.setPriority(7);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(textMessage);

            Message response = consumer.receive(10000);

            // this is ugly, but will do for this test. Man, I wish I could just pass
            // in a closure here...
            if (honorProperties)
            {
                performHeadersHonoredAssertions(response);
            }
            else
            {
                performHeadersNotHonoredAssertions(response);
            }
        }
        finally
        {
            // Grrrr.....
            try
            {
                if (consumerConnection != null)
                {
                    consumerConnection.close();
                }
            }
            catch (JMSException e)
            {
                // don't care, just let the producer be closed as well
            }

            try
            {
                if (producerConnection != null)
                {
                    producerConnection.close();
                }
            }
            catch (JMSException e)
            {
                // don't care
            }
        }

    }

    protected void performHeadersHonoredAssertions(final Message response) throws JMSException
    {
        assertNotNull(response);
        assertEquals("JMS Priority should've been honored.", 7, response.getJMSPriority());
        assertEquals("JMS Delivery mode should've been honored", DeliveryMode.PERSISTENT,
            response.getJMSDeliveryMode());
    }

    protected void performHeadersNotHonoredAssertions(final Message response) throws JMSException
    {
        assertNotNull(response);
        // default priority
        assertEquals("JMS Priority should have not been honored.", 4, response.getJMSPriority());
        assertEquals("JMS Delivery mode should have not been honored", DeliveryMode.NON_PERSISTENT,
            response.getJMSDeliveryMode());
    }
}
