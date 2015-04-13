/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction.xa;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.jms.support.JmsUtils;

public class JmsOutboundMessagesCounter implements TransactionScenarios.OutboundMessagesCounter
{

    private MessageConsumer consumer;
    private boolean initialized;
    private int numberOfMessagesArrived;
    private String brokerUrl;
    private Connection connection;


    public static JmsOutboundMessagesCounter createVerifierForBroker(int port)
    {
        return new JmsOutboundMessagesCounter("tcp://localhost:" + port);
    }

    private JmsOutboundMessagesCounter(String brokerUrl)
    {
        this.brokerUrl = brokerUrl;
    }

    public void initialize()
    {
        if (!this.initialized)
        {
            ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory();
            cf.setBrokerURL(brokerUrl);
            try
            {
                connection = cf.createConnection();
                connection.start();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                consumer = session.createConsumer(session.createQueue("out"));
            }
            catch (JMSException e)
            {
                throw new RuntimeException(e);
            }
            this.initialized = true;
        }
    }

    @Override
    public int numberOfMessagesThatArrived() throws Exception
    {
        initialize();
        while (true)
        {
            Message message = consumer.receive(1000);
            if (message != null)
            {
                numberOfMessagesArrived++;
            }
            else
            {
                break;
            }
        }
        return numberOfMessagesArrived;
    }

    @Override
    public void close()
    {
        JmsUtils.closeMessageConsumer(consumer);
        JmsUtils.closeConnection(connection);
        consumer = null;
        this.initialized = false;
        numberOfMessagesArrived = 0;
    }
}
