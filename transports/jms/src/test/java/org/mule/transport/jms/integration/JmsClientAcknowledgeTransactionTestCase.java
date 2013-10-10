/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Test jms using JmsClientAcknowledgeTransactionFactory
 */
public class JmsClientAcknowledgeTransactionTestCase extends AbstractJmsFunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-client-acknowledge-tx.xml";
    }

    @Test
    public void testJmsClientAcknowledgeTransaction() throws Exception
    {
        send(scenarioAcknowledge);
        receive(scenarioWithoutAcknowledge);
        receive(scenarioAcknowledge);
        receive(scenarioNotReceive);
    }

    Scenario scenarioAcknowledge = new NonTransactedScenario()
    {
        @Override
        public int getAcknowledge()
        {
            return Session.CLIENT_ACKNOWLEDGE;
        }

        @Override
        public void send(Session session, MessageProducer producer) throws JMSException
        {
            producer.send(session.createTextMessage(DEFAULT_INPUT_MESSAGE));
        }

        @Override
        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            Message message = consumer.receive(getTimeout());
            assertNotNull(message);
            message.acknowledge();
            return message;
        }
    };

    Scenario scenarioWithoutAcknowledge = new NonTransactedScenario()
    {
        @Override
        public int getAcknowledge()
        {
            return Session.CLIENT_ACKNOWLEDGE;
        }
    };
}
