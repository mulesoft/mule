/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
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
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.NamingException;

/**
 * <code>Jms11Support</code> is a template class to provide an abstraction to to
 * the JMS 1.1 API specification.
 */

public class Jms11Support implements JmsSupport
{
    protected Context context;
    protected boolean jndiDestinations = false;
    protected boolean forceJndiDestinations = false;
    protected JmsConnector connector;

    public Jms11Support(JmsConnector connector,
                        Context context,
                        boolean jndiDestinations,
                        boolean forceJndiDestinations)
    {
        this.connector = connector;
        this.context = context;
        this.jndiDestinations = jndiDestinations;
        this.forceJndiDestinations = forceJndiDestinations;
    }

    public Connection createConnection(ConnectionFactory connectionFactory, String username, String password)
        throws JMSException
    {
        if (connectionFactory == null)
        {
            throw new IllegalArgumentException("connectionFactory cannot be null");
        }
        return connectionFactory.createConnection(username, password);
    }

    public Connection createConnection(ConnectionFactory connectionFactory) throws JMSException
    {
        if (connectionFactory == null)
        {
            throw new IllegalArgumentException("connectionFactory cannot be null");
        }
        return connectionFactory.createConnection();
    }

    public Session createSession(Connection connection,
                                 boolean topic,
                                 boolean transacted,
                                 int ackMode,
                                 boolean noLocal) throws JMSException
    {
        return connection.createSession(transacted, (transacted ? Session.SESSION_TRANSACTED : ackMode));
    }

    public MessageProducer createProducer(Session session, Destination destination, boolean topic)
        throws JMSException
    {
        return session.createProducer(destination);
    }

    public MessageConsumer createConsumer(Session session, Destination destination, boolean topic)
        throws JMSException
    {
        return createConsumer(session, destination, null, false, null, topic);
    }

    public MessageConsumer createConsumer(Session session,
                                          Destination destination,
                                          String messageSelector,
                                          boolean noLocal,
                                          String durableName,
                                          boolean topic) throws JMSException
    {
        if (durableName == null)
        {
            if (topic)
            {
                return session.createConsumer(destination, messageSelector, noLocal);
            }
            else
            {
                return session.createConsumer(destination, messageSelector);
            }
        }
        else
        {
            if (topic)
            {
                return session.createDurableSubscriber((Topic)destination, durableName, messageSelector,
                    noLocal);
            }
            else
            {
                throw new JMSException(
                    "A durable subscriber name was set but the destination was not a Topic");
            }
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
                throw new IllegalArgumentException(
                    "Jndi Context name cannot be null when looking up a destination");
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

    protected Destination getJndiDestination(String name) throws JMSException
    {
        Object temp;
        try
        {
            temp = context.lookup(name);
        }
        catch (NamingException e)
        {
            throw new JMSException("Failed to look up destination: " + e.getMessage());
        }
        if (temp != null)
        {
            if (temp instanceof Destination)
            {
                return (Destination)temp;
            }
            else if (forceJndiDestinations)
            {
                throw new JMSException("JNDI destination not found with name: " + name);
            }
        }
        return null;
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

    public void send(MessageProducer producer, Message message, boolean topic) throws JMSException
    {
        send(producer, message, connector.isPersistentDelivery(), Message.DEFAULT_PRIORITY,
            Message.DEFAULT_TIME_TO_LIVE, topic);
    }

    public void send(MessageProducer producer, Message message, Destination dest, boolean topic)
        throws JMSException
    {
        send(producer, message, dest, connector.isPersistentDelivery(), Message.DEFAULT_PRIORITY,
            Message.DEFAULT_TIME_TO_LIVE, topic);
    }

    public void send(MessageProducer producer,
                     Message message,
                     boolean persistent,
                     int priority,
                     long ttl,
                     boolean topic) throws JMSException
    {
        producer.send(message, (persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT),
            priority, ttl);
    }

    public void send(MessageProducer producer,
                     Message message,
                     Destination dest,
                     boolean persistent,
                     int priority,
                     long ttl,
                     boolean topic) throws JMSException
    {
        producer.send(dest, message, (persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT),
            priority, ttl);
    }

}
