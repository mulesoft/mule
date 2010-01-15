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

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

/**
 * Tests a transactional exception strategy.
 */
public class JmsExceptionStrategyTestCase extends AbstractJmsFunctionalTestCase
{
    public static final String DEADLETTER_QUEUE_NAME = "dlq";

    public JmsExceptionStrategyTestCase(JmsVendorConfiguration config)
    {
        super(config);
        setTransacted(true);
    }

    protected String getConfigResources()
    {
        return "integration/jms-exception-strategy.xml";
    }

    @Test
    public void testTransactedRedeliveryToDLDestination() throws Exception
    {
        sendAndCommit(DEFAULT_INPUT_MESSAGE);
        // Verify message did _not_ get delivered to the outbound destination.
        receiveAndAssertNone();

        // Verify that an ExceptionMessage got sent to dead letter queue instead.
        Message message = receive(getJmsConfig().getDeadLetterDestinationName(), getTimeout(), null);

        Object obj = null;
        // ExceptionMessage got serialized by JMS provider
        if (message instanceof BytesMessage)
        {
            byte[] messageBytes = new byte[(int) ((BytesMessage) message).getBodyLength()];
            ((BytesMessage) message).readBytes(messageBytes);
            obj = SerializationUtils.deserialize(messageBytes);
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
        // Original JMS message was not serializable and toString() was called
        // instead
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
        // Some JMS providers do not allow custom properties to be set on JMS
        // messages
        if (dest != null)
        {
            assertEquals(getJmsConfig().getDeadLetterEndpoint(), dest);
        }
    }

    @Test
    public void testTransactedRedeliveryToDLDestinationRollback() throws Exception
    {
        sendAndCommit(DEFAULT_INPUT_MESSAGE);
        
        // Receive message but roll back transaction.
        receive(getJmsConfig().getDeadLetterDestinationName(), getTimeout(), new MessagePostProcessor() 
            {
                public void postProcess(Session session, Message message) throws JMSException
                {
                    session.rollback();
                }
            });

        // Receive message again and commit transaction.
        receive(getJmsConfig().getDeadLetterDestinationName(), getTimeout(), new MessagePostProcessor() 
        {
            public void postProcess(Session session, Message message) throws JMSException
            {
                session.commit();
            }
        });

        // Verify there is no more message to receive.
        Message output = receive(getJmsConfig().getDeadLetterDestinationName(), getSmallTimeout(), null);
        assertNull(output);
    }
}
