/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.integration;

import org.mule.tck.FunctionalTestCase;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;

public class JmsSingleTransactionRecieveAndSendTestCase extends FunctionalTestCase
{
    private Connection connection = null;
    private Session session = null;

    protected String getConfigResources()
    {
        return "providers/activemq/jms-single-tx-recieve-send-in-one-tx.xml";
    }

    public void testSingleTransactionBeginOrJoinAndAlwaysBegin() throws Exception
    {
        try
        {
            ConnectionFactory factory = new ActiveMQConnectionFactory(AbstractJmsFunctionalTestCase.DEFAULT_BROKER_URL);
            connection = factory.createConnection();
            connection.start();

            try
            {
                session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
                ActiveMQQueue destination = new ActiveMQQueue(AbstractJmsFunctionalTestCase.DEFAULT_INPUT_MQ_QUEUE_NAME);
                MessageProducer producer = null;
                try
                {
                    producer = session.createProducer(destination);
                    producer.send(session.createTextMessage(AbstractJmsFunctionalTestCase.DEFAULT_INPUT_MESSAGE));
                    session.commit();
                }
                finally
                {
                    if (producer != null)
                    {
                        producer.close();
                    }
                }

                destination = new ActiveMQQueue(AbstractJmsFunctionalTestCase.DEFAULT_OUTPUT_MQ_QUEUE_NAME);
                MessageConsumer consumer = null;
                try
                {
                    consumer = session.createConsumer(destination);
                    Message message = consumer.receive(AbstractJmsFunctionalTestCase.TIMEOUT);
                    assertNotNull(message);
                    assertTrue(TextMessage.class.isAssignableFrom(message.getClass()));
                    assertEquals(((TextMessage)message).getText(),
                        AbstractJmsFunctionalTestCase.DEFAULT_OUTPUT_MESSAGE);
                    session.commit();

                }
                finally
                {
                    if (consumer != null)
                    {
                        consumer.close();
                    }
                }
            }
            finally
            {
                if (session != null)
                {
                    session.close();
                }
            }
        }
        finally
        {
            if (connection != null)
            {
                connection.close();
            }
        }
    }

}
