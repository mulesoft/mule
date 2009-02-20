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

import org.mule.api.config.MuleProperties;
import org.mule.message.ExceptionMessage;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.junit.Test;

/**
 * Tests a transactional exception strategy.
 */
public class JmsExceptionStrategyTestCase extends AbstractJmsFunctionalTestCase
{
    public static final String DEADLETTER_QUEUE_NAME = "dead.letter";

    public JmsExceptionStrategyTestCase(JmsVendorConfiguration config)
    {
        super(config);
    }

    protected String getConfigResources()
    {
        return "integration/jms-exception-strategy.xml";
    }

    @Test
    public void testTransactedRedeliveryToDLDestination() throws Exception
    {
        send(scenarioDeadLetter);
        // Verify outbound message did _not_ get delivered.
        receive(scenarioNotReceive);
        // Verify message got sent to dead letter queue instead.
        receive(scenarioDeadLetter);
    }

    @Test
    public void testTransactedRedeliveryToDLDestinationRollback() throws Exception
    {
        send(scenarioDeadLetter);
        // Receive message but roll back transaction.
        receive(scenarioDeadLetterRollback);
        // Receive message again and commit transaction.
        receive(scenarioDeadLetter);
        // Verify there is no more message to receive.
        receive(scenarioDeadLetterNotReceive);
    }

    Scenario scenarioDeadLetter = new ScenarioDeadLetter();

    class ScenarioDeadLetter extends ScenarioCommit
    {
        // @Override
        public String getOutputDestinationName()
        {
            return DEADLETTER_QUEUE_NAME;
        }

        // @Override
        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            // Verify message got sent to dead letter queue.
            Message message = consumer.receive(getTimeout());
            assertNotNull(message);
            assertTrue("Message should be ObjectMessage but is " + message.getClass(),
                message instanceof ObjectMessage);
            Object obj = ((ObjectMessage) message).getObject();
            assertTrue(obj instanceof ExceptionMessage);
            // The payload should be the original message, not the reply message
            // since the FTC threw an exception.
            assertEquals(DEFAULT_INPUT_MESSAGE, ((ExceptionMessage) obj).getPayload());

            String dest = message.getStringProperty(MuleProperties.MULE_ENDPOINT_PROPERTY);
            assertNotNull(dest);
            assertEquals("jms://" + DEADLETTER_QUEUE_NAME, dest);

            applyTransaction(session);
            return message;
        }
    }

    Scenario scenarioDeadLetterRollback = new ScenarioDeadLetterRollback();

    class ScenarioDeadLetterRollback extends ScenarioDeadLetter
    {
        // @Override
        protected void applyTransaction(Session session) throws JMSException
        {
            session.rollback();
        }
    }

    Scenario scenarioDeadLetterNotReceive = new ScenarioDeadLetterNotReceive();

    class ScenarioDeadLetterNotReceive extends ScenarioDeadLetter
    {
        // @Override
        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            Message message = consumer.receive(getSmallTimeout());
            assertNull(message);
            return message;
        }
    }
}
