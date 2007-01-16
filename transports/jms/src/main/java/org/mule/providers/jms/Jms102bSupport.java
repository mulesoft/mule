/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jms;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;

/**
 * <code>Jms102bSupport</code> is a template class to provide an absstraction
 * to to the Jms 1.0.2b api specification.
 *
 */

public class Jms102bSupport extends Jms11Support
{
    public Jms102bSupport(JmsConnector connector,
                          Context context,
                          boolean jndiDestinations,
                          boolean forceJndiDestinations)
    {
        super(connector, context, jndiDestinations, forceJndiDestinations);
    }

    public Connection createConnection(ConnectionFactory connectionFactory, String username, String password)
            throws JMSException
    {
        if (connectionFactory == null)
        {
            throw new IllegalArgumentException("connectionFactory cannot be null");
        }
        if (connectionFactory instanceof QueueConnectionFactory)
        {
            return ((QueueConnectionFactory) connectionFactory).createQueueConnection(username, password);
        }
        else if (connectionFactory instanceof TopicConnectionFactory)
        {
            return ((TopicConnectionFactory) connectionFactory).createTopicConnection(username, password);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported ConnectionFactory type: "
                    + connectionFactory.getClass().getName());
        }
    }

    public Connection createConnection(ConnectionFactory connectionFactory) throws JMSException
    {
        if (connectionFactory == null)
        {
            throw new IllegalArgumentException("connectionFactory cannot be null");
        }
        if (connectionFactory instanceof QueueConnectionFactory)
        {
            return ((QueueConnectionFactory) connectionFactory).createQueueConnection();
        }
        else if (connectionFactory instanceof TopicConnectionFactory)
        {
            return ((TopicConnectionFactory) connectionFactory).createTopicConnection();
        }
        else
        {
            throw new IllegalArgumentException("Unsupported ConnectionFactory type: "
                    + connectionFactory.getClass().getName());
        }
    }

    public Session createSession(Connection connection, boolean topic, boolean transacted, int ackMode, boolean noLocal)
            throws JMSException
    {
        if (topic && connection instanceof TopicConnection)
        {
            return ((TopicConnection) connection).createTopicSession(noLocal, ackMode);
        }
        else if (connection instanceof QueueConnection)
        {
            return ((QueueConnection) connection).createQueueSession(transacted, ackMode);
        }
        else
        {
            throw new IllegalArgumentException("Connection and domain type do not match");
        }
    }

    public MessageConsumer createConsumer(Session session,
                                          Destination destination,
                                          String messageSelector,
                                          boolean noLocal,
                                          String durableName,
                                          boolean topic) throws JMSException
    {
        if (topic && session instanceof TopicSession)
        {
            if (durableName == null)
            {
                return ((TopicSession) session).createSubscriber((Topic) destination, messageSelector, noLocal);
            }
            else
            {
                return session.createDurableSubscriber((Topic) destination, durableName, messageSelector, noLocal);
            }
        }
        else if (session instanceof QueueSession)
        {
            if (messageSelector != null)
            {
                return ((QueueSession) session).createReceiver((Queue) destination, messageSelector);
            }
            else
            {
                return ((QueueSession) session).createReceiver((Queue) destination);
            }
        }
        else
        {
            throw new IllegalArgumentException("Session and domain type do not match");
        }
    }

    public MessageProducer createProducer(Session session, Destination dest, boolean topic) throws JMSException
    {
        if (topic && session instanceof TopicSession)
        {
            return ((TopicSession) session).createPublisher((Topic) dest);
        }
        else if (session instanceof QueueSession)
        {
            return ((QueueSession) session).createSender((Queue) dest);
        }
        else
        {
            throw new IllegalArgumentException("Session and domain type do not match");
        }
    }

    public Destination createDestination(Session session, String name, boolean topic) throws JMSException
    {
        if (session == null)
        {
            throw new IllegalArgumentException("Session cannot be null when creating a destination");
        }
        if (name == null)
        {
            throw new IllegalArgumentException("Destination name cannot be null when creating a destination");
        }

        if (jndiDestinations)
        {
            if (context == null)
            {
                throw new IllegalArgumentException("Jndi Context name cannot be null when looking up a destination");
            }
            Destination dest = getJndiDestination(name);
            if (dest != null)
            {
                return dest;
            }
            else if (forceJndiDestinations)
            {
                throw new JMSException("JNDI destination not found with name: " + name);
            }
        }

        if (topic)
        {
            return session.createTopic(name);
        }
        else
        {
            return session.createQueue(name);
        }
    }

    public Destination createTemporaryDestination(Session session, boolean topic) throws JMSException
    {
        if (session == null)
        {
            throw new IllegalArgumentException("Session cannot be null when creating a destination");
        }

        if (topic)
        {
            return session.createTemporaryTopic();
        }
        else
        {
            return session.createTemporaryQueue();
        }
    }

    public void send(MessageProducer producer, Message message, boolean persistent, int priority, long ttl, boolean topic)
            throws JMSException
    {
        if (topic && producer instanceof TopicPublisher)
        {
            ((TopicPublisher) producer).publish(
                    message,
                    (persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT),
                    priority,
                    ttl);
        }
        else if (producer instanceof QueueSender)
        {
            producer.send(
                    message,
                    (persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT),
                    priority,
                    ttl);
        }
        else
        {
            throw new IllegalArgumentException("Producer and domain type do not match");
        }
    }

    public void send(MessageProducer producer,
                     Message message,
                     Destination dest,
                     boolean persistent,
                     int priority,
                     long ttl,
                     boolean topic) throws JMSException
    {
        if (topic && producer instanceof TopicPublisher)
        {
            ((TopicPublisher) producer).publish(
                    (Topic) dest,
                    message,
                    (persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT),
                    priority,
                    ttl);
        }
        else if (producer instanceof QueueSender)
        {
            ((QueueSender) producer).send(
                    (Queue) dest,
                    message,
                    (persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT),
                    priority,
                    ttl);
        }
        else
        {
            throw new IllegalArgumentException("Producer and domain type do not match");
        }
    }
}
