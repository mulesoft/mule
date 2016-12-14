/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.support;

import static java.lang.String.format;
import static javax.jms.DeliveryMode.NON_PERSISTENT;
import static javax.jms.DeliveryMode.PERSISTENT;
import static javax.jms.Session.SESSION_TRANSACTED;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.extensions.jms.api.connection.JmsSpecification.JMS_1_0_2b;
import static org.mule.extensions.jms.api.connection.LookupJndiDestination.ALWAYS;
import static org.mule.extensions.jms.api.connection.LookupJndiDestination.TRY_ALWAYS;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.extensions.jms.api.connection.JmsSpecification;
import org.mule.extensions.jms.api.connection.LookupJndiDestination;
import org.mule.extensions.jms.api.destination.ConsumerType;
import org.mule.extensions.jms.api.destination.TopicConsumer;

import java.util.Optional;
import java.util.function.Function;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>Jms102bSupport</code> is a template class to provide an abstraction
 * to to the Jms 1.0.2b api specification.
 *
 * @since 4.0
 */
public class Jms102bSupport extends Jms11Support {

  private static final Logger LOGGER = LoggerFactory.getLogger(Jms102bSupport.class);

  public Jms102bSupport() {
    super();
  }

  public Jms102bSupport(LookupJndiDestination lookupJndiDestination, Function<String, Optional<Destination>> jndiObjectSupplier) {
    super(lookupJndiDestination, jndiObjectSupplier);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JmsSpecification getSpecification() {
    return JMS_1_0_2b;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection createConnection(ConnectionFactory connectionFactory, String username, String password)
      throws JMSException {

    checkArgument(connectionFactory != null, "connectionFactory cannot be null");

    if (connectionFactory instanceof QueueConnectionFactory) {
      return ((QueueConnectionFactory) connectionFactory).createQueueConnection(username, password);
    } else if (connectionFactory instanceof TopicConnectionFactory) {
      return ((TopicConnectionFactory) connectionFactory).createTopicConnection(username, password);
    } else {
      throw new IllegalArgumentException("Unsupported ConnectionFactory type: "
          + connectionFactory.getClass().getName());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection createConnection(ConnectionFactory connectionFactory) throws JMSException {
    checkArgument(connectionFactory != null, "connectionFactory cannot be null");

    if (connectionFactory instanceof QueueConnectionFactory) {
      return ((QueueConnectionFactory) connectionFactory).createQueueConnection();
    } else if (connectionFactory instanceof TopicConnectionFactory) {
      return ((TopicConnectionFactory) connectionFactory).createTopicConnection();
    } else {
      throw new IllegalArgumentException("Unsupported ConnectionFactory type: " + connectionFactory.getClass().getName());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Session createSession(Connection connection, boolean topic, boolean transacted, int ackMode)
      throws JMSException {
    checkArgument(connection != null, "Connection cannot be null");

    if (topic && connection instanceof TopicConnection) {
      return ((TopicConnection) connection).createTopicSession(transacted, (transacted ? SESSION_TRANSACTED : ackMode));
    } else if (connection instanceof QueueConnection) {
      // for transacted sessions the ackMode is always ignored, but
      // set it for readability (SESSION_TRANSACTION is recommended
      // for this case).
      return ((QueueConnection) connection).createQueueSession(transacted, (transacted ? SESSION_TRANSACTED : ackMode));
    } else {
      throw new IllegalArgumentException(
                                         "Connection and domain type do not match, connection is of type "
                                             + connection.getClass().getName());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MessageConsumer createConsumer(Session session, Destination destination, String messageSelector, ConsumerType type)
      throws JMSException {

    if (type.isTopic() && session instanceof TopicSession) {
      TopicConsumer topicConsumer = (TopicConsumer) type;
      if (!topicConsumer.isDurable()) {
        return ((TopicSession) session).createSubscriber((Topic) destination, messageSelector, topicConsumer.isNoLocal());
      } else {
        // DO NOT REMOVE THE CAST, breaks Weblogic
        return ((TopicSession) session).createDurableSubscriber((Topic) destination, topicConsumer.getSubscriptionName(),
                                                                messageSelector, topicConsumer.isNoLocal());
      }
    } else if (session instanceof QueueSession) {
      if (messageSelector != null) {
        return ((QueueSession) session).createReceiver((Queue) destination, messageSelector);
      } else {
        return ((QueueSession) session).createReceiver((Queue) destination);
      }
    } else {
      throw new IllegalArgumentException("Session and domain type do not match");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MessageProducer createProducer(Session session, Destination dest, boolean topic) throws JMSException {
    if (topic && session instanceof TopicSession) {
      return ((TopicSession) session).createPublisher((Topic) dest);
    } else if (session instanceof QueueSession) {
      return ((QueueSession) session).createSender((Queue) dest);
    } else {
      throw new IllegalArgumentException("Session and domain type do not match");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Destination createDestination(Session session, String name, boolean topic) throws JMSException {
    if (getLookupJndiDestination().equals(ALWAYS) || getLookupJndiDestination().equals(TRY_ALWAYS)) {
      Optional<Destination> destination = createDestinationFromJndi(name);
      if (destination.isPresent()) {
        return destination.get();
      }
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Using non-JNDI destination " + name + ", will create one now");
    }

    if (session == null) {
      throw new IllegalArgumentException("Session cannot be null when creating a destination");
    }
    if (isBlank(name)) {
      throw new IllegalArgumentException("Destination name cannot be null when creating a destination");
    }

    if (topic) {
      // DO NOT REMOVE THE CAST, BREAKS WEBLOGIC 8.X
      return ((TopicSession) session).createTopic(name);
    } else {
      // DO NOT REMOVE THE CAST, BREAKS WEBLOGIC 8.X
      return ((QueueSession) session).createQueue(name);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Destination createTemporaryDestination(Session session) throws JMSException {
    checkArgument(session != null, "Session cannot be null when creating a destination");
    // DO NOT REMOVE THE CAST, BREAKS WEBLOGIC 8.X
    return ((QueueSession) session).createTemporaryQueue();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send(MessageProducer producer, Message message, boolean persistent, int priority, long ttl, boolean topic)
      throws JMSException {
    if (LOGGER.isDebugEnabled()) {
      Destination dest = producer.getDestination();
      LOGGER.debug(format("Sending message to [%s], persistent:[%s], with priority:[%s] and ttl:[%s]",
                          dest instanceof Queue ? ((Queue) dest).getQueueName() : ((Topic) dest).getTopicName(),
                          persistent, priority, ttl));
    }

    int deliveryMode = persistent ? PERSISTENT : NON_PERSISTENT;

    if (topic && producer instanceof TopicPublisher) {
      ((TopicPublisher) producer).publish(message, deliveryMode, priority, ttl);

    } else if (producer instanceof QueueSender) {
      ((QueueSender) producer).send(message, deliveryMode, priority, ttl);

    } else {
      throw new IllegalArgumentException("Producer and domain type do not match");
    }
  }

}
