/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.message.ExceptionMessage;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Test;

/**
 * Tests a transactional exception strategy.
 */
public class JmsDeadLetterQueueTestCase extends AbstractJmsFunctionalTestCase
{
    public static final String DEADLETTER_QUEUE_NAME = "dlq";

    @Override
    protected String getConfigFile()
    {
        return "integration/jms-dead-letter-queue.xml";
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
        @Override
        public String getOutputDestinationName()
        {
            return DEADLETTER_QUEUE_NAME;
        }

        // @Override
        @Override
        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            // Verify message got sent to dead letter queue.
            Message message = consumer.receive(getTimeout());
            assertNotNull(message);

            Object obj = null;
            // ExceptionMessage got serialized by JMS provider
            if (message instanceof BytesMessage)
            {
                byte[] messageBytes = new byte[(int) ((BytesMessage) message).getBodyLength()];
                ((BytesMessage) message).readBytes(messageBytes);
                obj = muleContext.getObjectSerializer().deserialize(messageBytes);
            }
            // ExceptionMessage did not get serialized by JMS provider
            else if (message instanceof ObjectMessage)
            {
                obj = ((ObjectMessage) message).getObject();
            }
            else
            {
                fail("Message is an unexpected type: " + message.getClass().getName());
            }
            assertTrue(obj instanceof ExceptionMessage);

            // The payload should be the original message, not the reply message
            // since the FTC threw an exception.

            Object payload = ((ExceptionMessage) obj).getPayload();
            // Original JMS message was serializable
            if (payload instanceof TextMessage)
            {
                assertEquals(DEFAULT_INPUT_MESSAGE, ((TextMessage) payload).getText());
            }
            // Original JMS message was not serializable and toString() was called instead
            // (see AbstractExceptionListener.routeException() )
            else if (payload instanceof String)
            {
                assertEquals(DEFAULT_INPUT_MESSAGE, payload);
            }
            else
            {
                fail("Payload is an unexpected type: " + payload.getClass().getName());
            }

            String dest = message.getStringProperty(MuleProperties.MULE_ENDPOINT_PROPERTY);
            // Some JMS providers do not allow custom properties to be set on JMS messages
            if (dest != null)
            {
                assertEquals("jms://" + DEADLETTER_QUEUE_NAME, dest);
            }

            applyTransaction(session);
            return message;
        }
    }

    Scenario scenarioDeadLetterRollback = new ScenarioDeadLetterRollback();

    class ScenarioDeadLetterRollback extends ScenarioDeadLetter
    {
        // @Override
        @Override
        protected void applyTransaction(Session session) throws JMSException
        {
            session.rollback();
        }
    }

    Scenario scenarioDeadLetterNotReceive = new ScenarioDeadLetterNotReceive();

    class ScenarioDeadLetterNotReceive extends ScenarioDeadLetter
    {
        // @Override
        @Override
        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            Message message = consumer.receive(getSmallTimeout());
            assertNull(message);
            return message;
        }
    }
}
