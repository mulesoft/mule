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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * Testing durable topic with external subscriber
 */
public class JmsDurableTopicSingleTxTestCase extends JmsDurableTopicTestCase
{

    public static final String TOPIC_QUEUE_NAME = "durable.broadcast";

    protected String getConfigResources()
    {
        return "providers/activemq/jms-durable-topic-single-tx.xml";
    }

    /**
     * @throws Exception
     */
    public void testProviderDurableSubscriber() throws Exception
    {
        setClientId("Client1");
        receive(scenarioNotReceive);
        setClientId("Client2");
        receive(scenarioNotReceive);

        setClientId("Sender");
        send(scenarioCommit);

        setClientId("Client1");
        receive(scenarioCommit);
        receive(scenarioNotReceive);
        setClientId("Client2");
        receive(scenarioRollback);
        receive(scenarioCommit);
        receive(scenarioNotReceive);

    }

    AbstractJmsFunctionalTestCase.Scenario scenarioCommit = new AbstractJmsFunctionalTestCase.AbstractScenario()
    {

        public String getOutputQueue()
        {
            return TOPIC_QUEUE_NAME;
        }

        public void send(Session session, MessageProducer producer) throws JMSException
        {
            producer.send(session.createTextMessage(DEFAULT_INPUT_MESSAGE));
            session.commit();
        }

        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            Message message = consumer.receive(TIMEOUT);
            assertNotNull(message);
            session.commit();
            return message;
        }

        public boolean isTransacted()
        {
            return true;
        }
    };

    AbstractJmsFunctionalTestCase.Scenario scenarioRollback = new AbstractJmsFunctionalTestCase.AbstractScenario()
    {

        public String getOutputQueue()
        {
            return TOPIC_QUEUE_NAME;
        }

        public void send(Session session, MessageProducer producer) throws JMSException
        {
            producer.send(session.createTextMessage(DEFAULT_INPUT_MESSAGE));
            session.rollback();
        }

        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            Message message = consumer.receive(TIMEOUT);
            assertNotNull(message);
            session.rollback();
            return message;
        }

        public boolean isTransacted()
        {
            return true;
        }

    };


    AbstractJmsFunctionalTestCase.Scenario scenarioNotReceive = new AbstractJmsFunctionalTestCase.AbstractScenario()
    {

        public String getOutputQueue()
        {
            return TOPIC_QUEUE_NAME;
        }

        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            Message message = consumer.receive(SMALL_TIMEOUT);
            assertNull(message);
            return message;
        }

        public boolean isTransacted()
        {
            return true;
        }

    };

}
