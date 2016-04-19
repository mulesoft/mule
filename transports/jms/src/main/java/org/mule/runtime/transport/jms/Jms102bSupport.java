/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;

import java.text.MessageFormat;

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

/**
 * <code>Jms102bSupport</code> is a template class to provide an absstraction
 * to to the Jms 1.0.2b api specification.
 *
 */

public class Jms102bSupport extends Jms11Support
{
    public Jms102bSupport(JmsConnector connector)
    {
        super(connector);
    }

    @Override
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

    @Override
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

    @Override
    public Session createSession(Connection connection, boolean topic, boolean transacted,
        int ackMode, boolean noLocal) throws JMSException
    {
        if (connection == null)
        {
            throw new IllegalArgumentException("Connection is null");
        }
        if (topic && connection instanceof TopicConnection)
        {
            return ((TopicConnection) connection).createTopicSession(noLocal, ackMode);
        }
        else if (connection instanceof QueueConnection)
        {
            // for transacted sessions the ackMode is always ignored, but
            // set it for readability (SESSION_TRANSACTION is recommented
            // for this case).
            return ((QueueConnection) connection).createQueueSession(
                    transacted, (transacted ? Session.SESSION_TRANSACTED : ackMode));
        }
        else
        {
            throw new IllegalArgumentException("Connection and domain type do not match, connection is of type " + connection.getClass().getName());
        }
    }

    @Override
    public MessageConsumer createConsumer(Session session,
                                          Destination destination,
                                          String messageSelector,
                                          boolean noLocal,
                                          String durableName,
                                          boolean topic, ImmutableEndpoint endpoint) throws JMSException
    {
        if (topic && session instanceof TopicSession)
        {
            if (durableName == null)
            {
                return ((TopicSession) session).createSubscriber((Topic) destination, messageSelector, noLocal);
            }
            else
            {
                // DO NOT REMOVE THE CAST, breaks Weblogic
                return ((TopicSession) session).createDurableSubscriber((Topic) destination, durableName, messageSelector, noLocal);
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

    @Override
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

    @Override
    public Destination createDestination(Session session, String name, boolean topic, ImmutableEndpoint endpoint) throws JMSException
    {
        if (connector.isJndiDestinations())
        {
            try
            {
                Destination dest = getJndiDestination(name);
                if (dest != null)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(MessageFormat.format("Destination {0} located in JNDI, will use it now", name));
                    }
                    return dest;
                }
                else
                {
                    throw new JMSException("JNDI destination not found with name: " + name);
                }
            }
            catch (JMSException e)
            {
                if (connector.isForceJndiDestinations())
                {
                    throw e;
                }
                else
                {
                    logger.warn("Unable to look up JNDI destination " + name + ": " + e.getMessage());
                }
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Using non-JNDI destination " + name + ", will create one now");
        }

        if (session == null)
        {
            throw new IllegalArgumentException("Session cannot be null when creating a destination");
        }
        if (name == null)
        {
            throw new IllegalArgumentException("Destination name cannot be null when creating a destination");
        }

        if (topic)
        {
            // DO NOT REMOVE THE CAST, BREAKS WEBLOGIC 8.X
            return ((TopicSession) session).createTopic(name);
        }
        else
        {
            // DO NOT REMOVE THE CAST, BREAKS WEBLOGIC 8.X
            return ((QueueSession) session).createQueue(name);
        }
    }

    @Override
    public Destination createTemporaryDestination(Session session, boolean topic) throws JMSException
    {
        if (session == null)
        {
            throw new IllegalArgumentException("Session cannot be null when creating a destination");
        }

        if (topic)
        {
            // DO NOT REMOVE THE CAST, BREAKS WEBLOGIC 8.X
            return ((TopicSession) session).createTemporaryTopic();
        }
        else
        {
            // DO NOT REMOVE THE CAST, BREAKS WEBLOGIC 8.X
            return ((QueueSession) session).createTemporaryQueue();
        }
    }

    @Override
    public void send(MessageProducer producer, Message message, boolean persistent, int priority, long ttl, boolean topic, ImmutableEndpoint endpoint)
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
            // DO NOT REMOVE THIS CAST, it breaks Weblogic
            ((QueueSender) producer).send(
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

    @Override
    public void send(MessageProducer producer,
                     Message message,
                     Destination dest,
                     boolean persistent,
                     int priority,
                     long ttl,
                     boolean topic, ImmutableEndpoint endpoint) throws JMSException
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
