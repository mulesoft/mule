/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.reliability;

import org.mule.runtime.transport.jms.integration.AbstractJmsFunctionalTestCase;
import org.mule.runtime.transport.jms.integration.JmsVendorConfiguration;
import org.mule.runtime.transport.jms.integration.activemq.ActiveMQJmsConfiguration;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

public abstract class AbstractJmsReliabilityTestCase extends AbstractJmsFunctionalTestCase
{
    protected int acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
    protected int deliveryMode = DeliveryMode.PERSISTENT;
        
    // These are used by the receiver only, not the sender
    protected Connection connection;
    protected Session session;
    protected MessageConsumer consumer;
        
    public AbstractJmsReliabilityTestCase()
    {
        setMultipleProviders(false);
    }
    
    @Override
    protected void doTearDown() throws Exception
    {
        closeConsumer();
        super.doTearDown();
    }
    
    protected void closeConsumer() throws Exception
    {
        if (consumer != null)
        {
            consumer.close();
            consumer = null;
            session.close();
            session = null;
            connection.close();
            connection = null;
        }
    }

    protected void putMessageOnQueue(String queueName) throws Exception
    {
        JmsVendorConfiguration jmsConfig = new ActiveMQJmsConfiguration();
        Connection connection = null;
        try
        {
            connection = jmsConfig.getConnection(false, false);
            connection.start();
            Session session = null;
            try
            {
                session = connection.createSession(false, acknowledgeMode);
                Destination destination = session.createQueue(queueName);
                MessageProducer producer = null;
                try
                {
                    producer = session.createProducer(destination);
                    producer.setDeliveryMode(deliveryMode);
                    Message msg = session.createTextMessage(AbstractJmsFunctionalTestCase.DEFAULT_INPUT_MESSAGE);
                    msg.setJMSExpiration(0);
                    producer.send(msg);
                }
                finally
                {
                    if (producer != null)
                    {
                        producer.close();
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
    
    protected Message readMessageFromQueue(String queueName) throws Exception
    {
        if (consumer == null)
        {
            createConsumer(queueName);
        }        
        return consumer.receive(getTimeout());
    }

    protected void listenOnQueue(String queueName, MessageListener listener) throws Exception
    {
        if (consumer == null)
        {
            createConsumer(queueName);
        }        
        consumer.setMessageListener(listener);
    }
    
    protected void createConsumer(String queueName) throws Exception
    {
        connection = getConnection(false, false);
        connection.start();
        session = connection.createSession(false, acknowledgeMode);
        consumer = session.createConsumer(session.createQueue(queueName));
    }
}


