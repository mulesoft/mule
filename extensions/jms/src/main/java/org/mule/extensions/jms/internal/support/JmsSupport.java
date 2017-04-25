/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.support;

import org.mule.extensions.jms.internal.config.InternalAckMode;
import org.mule.extensions.jms.api.connection.JmsSpecification;
import org.mule.extensions.jms.api.connection.LookupJndiDestination;
import org.mule.extensions.jms.api.destination.ConsumerType;
import org.mule.extensions.jms.api.exception.DestinationNotFoundException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueSender;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

/**
 * <code>JmsSupport</code> is an interface that provides a polymorphic facade to
 * the JMS 1.0.2b, 1.1 and 2.0 API specifications. this interface is not intended for
 * general purpose use and should only be used with the Mule JMS connector.
 *
 * @since 4.0
 */
public interface JmsSupport {

  /**
   * Creates a new {@link Connection}
   *
   * @param connectionFactory the {@link ConnectionFactory} to use for creating the {@link Connection}
   * @return a new {@link Connection}
   * @throws JMSException if an error occurs during connection creation
   */
  Connection createConnection(ConnectionFactory connectionFactory) throws JMSException;

  /**
   * Creates a new {@link Connection} with custom user/password authentication
   *
   * @param connectionFactory the {@link ConnectionFactory} to use for creating the {@link Connection}
   * @param username the credentials username
   * @param password the credentials password
   * @return a new {@link Connection}
   * @throws JMSException if an error occurs during connection creation
   */
  Connection createConnection(ConnectionFactory connectionFactory, String username, String password)
      throws JMSException;

  /**
   * Creates a new {@link Session} for the given {@link Connection}
   *
   * @param connection the {@link Connection} to use for creating the {@link Session}
   * @param topic if {@code true} the {@link Session} created will be a {@link TopicSession}.
   *                This distinction is made only for {@link JmsSpecification#JMS_1_0_2b}
   * @param transacted indicates whether the session will use a local transaction
   * @param ackMode the {@link Session} {@link InternalAckMode}
   * @return a new {@link Session}
   * @throws JMSException if an error occurs during connection creation
   */
  Session createSession(Connection connection, boolean topic, boolean transacted, int ackMode)
      throws JMSException;

  /**
   * Creates a new JMS {@link Destination} with the given {@code name}.
   * If {@link LookupJndiDestination#ALWAYS} or {@link LookupJndiDestination#TRY_ALWAYS} where configured,
   * this {@code name} will be used for performing the JNDI lookup
   *
   * @param session the {@link Session} used to create the {@link Destination}
   * @param name the name of the destination
   * @param topic if {@code true}, a {@link Topic} {@link Destination} will be created,
   *              otherwise the {@link Queue} default is used
   * @return a new {@link Destination}
   * @throws JMSException if an error occurs during the creation of the destination
   * @throws DestinationNotFoundException if {@link LookupJndiDestination#ALWAYS} is configured
   * and no {@link Destination} with the given {@code name} is found
   */
  Destination createDestination(Session session, String name, boolean topic) throws JMSException, DestinationNotFoundException;

  /**
   * Creates an anonymous temporary {@link Queue} {@link Destination}
   *
   * @param session the {@link Session} used to create the {@link Destination}
   * @return a new temporary {@link Queue} {@link Destination}
   * @throws JMSException if an error occurs during destination creation
   */
  Destination createTemporaryDestination(Session session) throws JMSException;

  /**
   * Creates a new JMS {@link MessageProducer} using the given {@link Session}
   *
   * @param session the {@link Session} used to create the {@link MessageProducer}
   * @param destination the {@link Destination} to where the {@link Message}s will be published
   * @param topic if {@code true} the given {@link Destination} has a {@link Topic} destination type.
   *                This distinction is made only for {@link JmsSpecification#JMS_1_0_2b} in order to decide whether
   *                to create a {@link TopicPublisher} or a {@link QueueSender}
   * @return a new {@link MessageProducer} for the given {@link Destination}
   * @throws JMSException if an error occurs while creating the consumer
   */

  MessageProducer createProducer(Session session, Destination destination, boolean topic)
      throws JMSException;

  /**
   * Sends the given {@link Message} to the indicated {@link Destination} using the parametrized configurations
   *
   * @param producer the {@link MessageProducer} to use for sending the message
   * @param message the {@link Message} to send
   * @param persistent {@code true} if {@link DeliveryMode#PERSISTENT} should be used
   * @param priority the {@link Message#getJMSPriority} to be set on send
   * @param ttl the message's lifetime (in milliseconds)
   * @param topic used by {@link JmsSpecification#JMS_1_0_2b} to decide whether to use the {@link MessageProducer}
   *              as {@link TopicPublisher} or a {@link QueueSender}
   * @throws JMSException if an error occurs during messsage sending
   */
  void send(MessageProducer producer, Message message, boolean persistent, int priority,
            long ttl, boolean topic)
      throws JMSException;

  /**
   * Creates a new JMS {@link MessageConsumer} using the given {@link Session}
   *
   * @param session the {@link Session} used to create the {@link MessageConsumer}
   * @param destination the {@link Destination} from which {@link Message}s will be consumed
   * @param messageSelector a JMS selector string for filtering incoming {@link Message}s. Empty or {@code null} implies no filtering
   * @param type the {@link ConsumerType} to use based on the {@link Destination} type
   * @return a new {@link MessageConsumer} for the given {@link Destination}
   * @throws JMSException if an error occurs while creating the consumer
   */
  MessageConsumer createConsumer(Session session, Destination destination, String messageSelector, ConsumerType type)
      throws JMSException;

  /**
   * @return {@code this} implementation {@link JmsSpecification}
   */
  JmsSpecification getSpecification();

}
