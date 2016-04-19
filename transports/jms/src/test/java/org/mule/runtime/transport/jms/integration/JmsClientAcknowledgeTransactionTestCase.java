/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration;

import static org.junit.Assert.assertNotNull;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Test jms using JmsClientAcknowledgeTransactionFactory
 */
public class JmsClientAcknowledgeTransactionTestCase extends AbstractJmsFunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "integration/jms-client-acknowledge-tx.xml";
    }

    @Test
    @Ignore("MULE-6926: flaky test")
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
