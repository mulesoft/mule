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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.junit.Test;

/**
 * Test jms using JmsClientAcknowledgeTransactionFactory
 */
public class JmsClientAcknowledgeTransactionTestCase extends AbstractJmsFunctionalTestCase
{

    public JmsClientAcknowledgeTransactionTestCase(JmsVendorConfiguration config)
    {
        super(config);
        System.out.println("using jms provider : " + config.getProviderName());
    }

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

        public void send(Session session, MessageProducer producer) throws JMSException
        {
            producer.send(session.createTextMessage(DEFAULT_INPUT_MESSAGE));
        }

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
