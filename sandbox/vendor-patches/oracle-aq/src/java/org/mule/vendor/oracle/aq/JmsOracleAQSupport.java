/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 */
package org.mule.vendor.oracle.aq;

import be.kmi_irm.labo.messaging.mule.AQSessionDelegate;
import oracle.AQ.AQSession;
import oracle.jms.AQjmsSession;

import javax.jms.*;
import javax.naming.Context;

/**
 * @author henks
 * 
 */
public class JmsOracleAQSupport extends Jms11Support {
	
	private String schema = null;

    public Connection createConnection(ConnectionFactory connectionFactory) throws JMSException
    {
        if (connectionFactory == null)
        {
            throw new IllegalArgumentException("connectionFactory cannot be null");
        }
        if (connectionFactory instanceof QueueConnectionFactory)
        {
            return ((QueueConnectionFactory) connectionFactory).createQueueConnection();
        } else if (connectionFactory instanceof TopicConnectionFactory)
        {
            return ((TopicConnectionFactory) connectionFactory).createTopicConnection();
        } else
        {
            throw new IllegalArgumentException("Unsupported ConnectionFactory type: " + connectionFactory.getClass().getName());
        }
    }


	/**
	 * @param context
	 * @param jndiDestinations
	 * @param forceJndiDestinations
	 */
	public JmsOracleAQSupport(Context context,
			boolean jndiDestinations, boolean forceJndiDestinations, String schema) {
		super(context, jndiDestinations, forceJndiDestinations);
		this.schema = schema;
	}

	public JmsOracleAQSupport(String schema) {
		super(null, false, false);
		this.schema = schema;
	}

	
	public Session createSession(Connection connection, boolean transacted,
			int ackMode, boolean noLocal) throws JMSException {
		if (connection instanceof QueueConnection) {
			Session sess = ((QueueConnection) connection).createQueueSession(
					transacted, ackMode);
			return sess;
		}
		else {
			return null;
		}
	}

	public MessageConsumer createConsumer(Session session,
			Destination destination, String messageSelector, boolean noLocal,
			String durableName) throws JMSException {
		
		if (session instanceof AQSessionDelegate) {
			session = ((AQSessionDelegate)session).getSession();
		}
		
		if (destination instanceof Queue) {
			if (session instanceof XAQueueSession
					|| session instanceof QueueSession) {
				if (messageSelector != null) {
					return ((QueueSession) session).createReceiver(
							(Queue) destination, messageSelector);
				} else {
					return ((QueueSession) session)
							.createReceiver((Queue) destination);
				}
			}
		} else {
			if (session instanceof XATopicSession
					|| session instanceof TopicSession) {
				if (durableName == null) {
					return ((TopicSession) session).createSubscriber(
							(Topic) destination, messageSelector, noLocal);
				} else {
					return ((TopicSession) session).createDurableSubscriber(
							(Topic) destination, messageSelector, durableName,
							noLocal);
				}
			}
		}
		throw new IllegalArgumentException(
				"Session and domain type do not match");
	}

	public MessageProducer createProducer(Session session, Destination dest)
			throws JMSException {
		if (dest instanceof Queue) {
			if (session instanceof XAQueueSession
					|| session instanceof QueueSession) {
				return ((QueueSession) session).createSender((Queue) dest);
			}
		} else {
			if (session instanceof XATopicSession
					|| session instanceof TopicSession) {
				return ((TopicSession) session).createPublisher((Topic) dest);
			}
		}
		throw new IllegalArgumentException(
				"Session and domain type do not match");
	}

	public Destination createDestination(Session session, String name,
			boolean topic) throws JMSException {
		if (session == null) {
			throw new IllegalArgumentException(
					"Session cannot be null when creating a destination");
		}
		if (name == null) {
			throw new IllegalArgumentException(
					"Destination name cannot be null when creating a destination");
		}
		if (!topic) {
			if (jndiDestinations) {
				if (context == null) {
					throw new IllegalArgumentException(
							"Jndi Context name cannot be null when looking up a destination");
				}
				Destination dest = getJndiDestination(name);
				if (dest != null) {
					return dest;
				} else if (forceJndiDestinations) {
					throw new JMSException(
							"JNDI destination not found with name: " + name);
				}
			}
			if (session instanceof AQSessionDelegate) {
				AQjmsSession sess = (AQjmsSession)((AQSessionDelegate)session).getSession();
				return sess.getQueue("stephane", name);
			} else {
				return ((AQjmsSession) session).getQueue("stephane", name);
			}
		} else {
			if (session instanceof XATopicSession
					|| session instanceof TopicSession) {
				return ((TopicSession) session).createTopic(name);
			} else {
				return session.createTopic(name);
			}
		}
	}

	public void send(MessageProducer producer, Message message)
			throws JMSException {
		if (producer instanceof QueueSender) {
			((QueueSender) producer).send(message);
		} else {
			((TopicPublisher) producer).publish(message);
		}
	}

	public void send(MessageProducer producer, Message message, Destination dest)
			throws JMSException {
		if (producer instanceof QueueSender) {
			((QueueSender) producer).send((Queue) dest, message);
		} else {
			((TopicPublisher) producer).publish((Topic) dest, message);
		}
	}

	public void send(MessageProducer producer, Message message, int ackMode,
			int priority, long ttl) throws JMSException {
		if (producer instanceof QueueSender) {
			((QueueSender) producer).send(message, ackMode, priority, ttl);
		} else {
			((TopicPublisher) producer)
					.publish(message, ackMode, priority, ttl);
		}
	}

	public void send(MessageProducer producer, Message message,
			Destination dest, int ackMode, int priority, long ttl)
			throws JMSException {
		if (producer instanceof QueueSender) {
			((QueueSender) producer).send((Queue) dest, message, ackMode,
					priority, ttl);
		} else {
			((TopicPublisher) producer).publish((Topic) dest, message, ackMode,
					priority, ttl);
		}
	}
	
	public String getSchema() {
		return schema;
	}
	public void setSchema(String schema) {
		this.schema = schema;
	}
}