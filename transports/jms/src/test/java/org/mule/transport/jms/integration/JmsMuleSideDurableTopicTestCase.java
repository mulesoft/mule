/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JmsMuleSideDurableTopicTestCase extends AbstractJmsFunctionalTestCase
{

    public static final String CONNECTOR1_NAME = "jmsConnectorC1";

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-muleside-durable-topic.xml";
    }

    @Test
    public void testMuleDurableSubscriber() throws Exception
    {
        send(scenarioNoTx);
        receive(scenarioNoTx);
        receive(scenarioNoTx);
        muleContext.getRegistry().lookupConnector(CONNECTOR1_NAME).stop();
        assertEquals(muleContext.getRegistry().lookupConnector(CONNECTOR1_NAME).isStarted(), false);
        logger.info(CONNECTOR1_NAME + " is stopped");
        send(scenarioNoTx);
        muleContext.getRegistry().lookupConnector(CONNECTOR1_NAME).start();
        logger.info(CONNECTOR1_NAME + " is started");
        receive(scenarioNoTx);
        receive(scenarioNoTx);
    }

    Scenario scenarioNoTx = new NonTransactedScenario()
    {
        @Override
        public String getInputDestinationName()
        {
            return getJmsConfig().getBroadcastDestinationName();
        }

        @Override
        public void send(Session session, MessageProducer producer) throws JMSException
        {
            // publish and send is the same for ActiveMQ
            producer.send(session.createTextMessage(DEFAULT_INPUT_MESSAGE));

        }

        @Override
        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            Message message = consumer.receive(getTimeout());
            assertNotNull(message);
            assertTrue(TextMessage.class.isAssignableFrom(message.getClass()));
            assertEquals(((TextMessage) message).getText(), DEFAULT_OUTPUT_MESSAGE);
            return message;
        }
    };

    @Override
    public void send(Scenario scenario) throws Exception
    {
        TopicConnection connection = null;
        try
        {
            connection = (TopicConnection) getConnection(true, false);
            connection.start();
            TopicSession session = null;
            try
            {
                session = connection.createTopicSession(scenario.isTransacted(), scenario.getAcknowledge());
                Topic destination = session.createTopic(scenario.getInputDestinationName());
                TopicPublisher publisher = null;
                try
                {
                    publisher = session.createPublisher(destination);
                    publisher.setDeliveryMode(DeliveryMode.PERSISTENT);
                    scenario.send(session, publisher);
                }
                finally
                {
                    if (publisher != null)
                    {
                        publisher.close();
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
