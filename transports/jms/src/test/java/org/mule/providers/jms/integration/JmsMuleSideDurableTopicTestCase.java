/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jms.integration;


import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTopic;

public class JmsMuleSideDurableTopicTestCase extends AbstractJmsFunctionalTestCase
{

    public static final String TOPIC_QUEUE_NAME = "durable.broadcast";
    public static final String CONNECTOR1_NAME = "jmsConnectorC1";

    protected String getConfigResources()
    {
        return "providers/activemq/jms-muleside-durable-topic.xml";
    }

    public void testMuleDurableSubscriber() throws Exception
    {
        send(scenarioNoTx);
        receive(scenarioNoTx);
        receive(scenarioNoTx);
        managementContext.getRegistry().lookupConnector(CONNECTOR1_NAME).stop();
        assertEquals(managementContext.getRegistry().lookupConnector(CONNECTOR1_NAME).isStarted(), false);
        logger.info(CONNECTOR1_NAME + " is stopped");
        send(scenarioNoTx);
        managementContext.getRegistry().lookupConnector(CONNECTOR1_NAME).start();
        logger.info(CONNECTOR1_NAME + " is started");
        receive(scenarioNoTx);
        receive(scenarioNoTx);

    }

    Scenario scenarioNoTx = new AbstractScenario()
    {
        public String getInputQueue()
        {
            return TOPIC_QUEUE_NAME;
        }

        public void send(Session session, MessageProducer producer) throws JMSException
        {
            //publish and send is the same for ActiveMQ
            producer.send(session.createTextMessage(DEFAULT_INPUT_MESSAGE));

        }

        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            Message message = consumer.receive(TIMEOUT);
            assertNotNull(message);
            assertTrue(TextMessage.class.isAssignableFrom(message.getClass()));
            assertEquals(((TextMessage) message).getText(), DEFAULT_OUTPUT_MESSAGE);
            return message;
        }

    };


    public void send(Scenario scenario) throws Exception
    {
        TopicConnection connection = null;
        try
        {
            TopicConnectionFactory factory = new ActiveMQConnectionFactory(scenario.getBrokerUrl());
            connection = factory.createTopicConnection();
            connection.start();
            TopicSession session = null;
            try
            {
                session = connection.createTopicSession(scenario.isTransacted(), scenario.getAcknowledge());
                ActiveMQTopic destination = new ActiveMQTopic(scenario.getInputQueue());
                TopicPublisher publisher = null;
                try
                {
                    publisher = session.createPublisher(destination);
                    publisher.setDeliveryMode(DeliveryMode.PERSISTENT);
                    scenario.send(session, publisher);
                }
                catch (Exception e)
                {
                    throw e;
                }
                finally
                {
                    if (publisher != null)
                    {
                        publisher.close();
                    }
                }
            }
            catch (Exception e)
            {
                throw e;
            }
            finally
            {
                if (session != null)
                {
                    session.close();
                }
            }
        }
        catch (Exception e)
        {
            throw e;
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
