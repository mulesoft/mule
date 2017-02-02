/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.support;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static javax.jms.DeliveryMode.NON_PERSISTENT;
import static javax.jms.DeliveryMode.PERSISTENT;
import static javax.jms.Session.SESSION_TRANSACTED;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.extensions.jms.api.connection.JmsSpecification.JMS_2_0;
import static org.mule.extensions.jms.api.connection.LookupJndiDestination.ALWAYS;
import static org.mule.extensions.jms.api.connection.LookupJndiDestination.NEVER;
import static org.mule.extensions.jms.api.connection.LookupJndiDestination.TRY_ALWAYS;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.connection.JmsSpecification;
import org.mule.extensions.jms.api.connection.LookupJndiDestination;
import org.mule.extensions.jms.api.destination.ConsumerType;
import org.mule.extensions.jms.api.destination.TopicConsumer;
import org.mule.extensions.jms.api.exception.DestinationNotFoundException;

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
import javax.jms.Session;
import javax.jms.Topic;

import org.slf4j.Logger;

/**
 * <code>Jms20Support</code> is a template class to provide an abstraction to to
 * the JMS 2.0 API specification.
 *
 * @since 4.0
 */
public class Jms20Support implements JmsSupport {

  private Logger LOGGER = getLogger(Jms20Support.class);

  private final Function<String, Optional<Destination>> jndiObjectSupplier;
  private final LookupJndiDestination lookupJndiDestination;

  public Jms20Support() {
    this(NEVER, (name) -> null);
  }

  public Jms20Support(LookupJndiDestination lookupJndiDestination, Function<String, Optional<Destination>> jndiObjectSupplier) {
    checkArgument(lookupJndiDestination != null, "The LookupJndiDestination cannot be null");
    checkArgument(jndiObjectSupplier != null, "The JndiObjectSupplier cannot be null");
    this.lookupJndiDestination = lookupJndiDestination;
    this.jndiObjectSupplier = jndiObjectSupplier;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JmsSpecification getSpecification() {
    return JMS_2_0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection createConnection(ConnectionFactory connectionFactory, String username, String password)
      throws JMSException {
    checkArgument(connectionFactory != null, "connectionFactory cannot be null");
    return connectionFactory.createConnection(username, password);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection createConnection(ConnectionFactory connectionFactory) throws JMSException {
    if (connectionFactory == null) {
      throw new IllegalArgumentException("connectionFactory cannot be null");
    }
    return connectionFactory.createConnection();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Session createSession(Connection connection, boolean topic, boolean transacted, int ackMode)
      throws JMSException {
    return connection.createSession(transacted, (transacted ? SESSION_TRANSACTED : ackMode));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MessageProducer createProducer(Session session, Destination destination, boolean topic)
      throws JMSException {
    return session.createProducer(destination);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MessageConsumer createConsumer(Session session, Destination destination, String messageSelector, ConsumerType type)
      throws JMSException {

    if (!type.isTopic()) {
      return session.createConsumer(destination, messageSelector);
    }

    TopicConsumer topicConsumer = (TopicConsumer) type;
    // Shared Consumer exists in JMS 2.0 only
    if (topicConsumer.isDurable() && topicConsumer.isShared()) {
      return session.createSharedDurableConsumer((Topic) destination, topicConsumer.getSubscriptionName(), messageSelector);
    }

    if (topicConsumer.isDurable()) {
      return session.createDurableSubscriber((Topic) destination, topicConsumer.getSubscriptionName(), messageSelector,
                                             topicConsumer.isNoLocal());
    }

    if (topicConsumer.isShared()) {
      return session.createSharedConsumer((Topic) destination, topicConsumer.getSubscriptionName(), messageSelector);
    }

    return session.createConsumer(destination, messageSelector, topicConsumer.isNoLocal());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Destination createDestination(Session session, String name, boolean topic)
      throws JMSException, DestinationNotFoundException {

    if (getLookupJndiDestination().equals(ALWAYS) || getLookupJndiDestination().equals(TRY_ALWAYS)) {
      Optional<Destination> destination = createDestinationFromJndi(name);
      if (destination.isPresent()) {
        return destination.get();

      } else if (getLookupJndiDestination().equals(ALWAYS)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(format("Failed to find JNDI destination [%s], but destination origin was forced as ALWAYS use JNDI."
              + " We have to stop execution.", name));

        }

        throw new DestinationNotFoundException(format("Failed to find JNDI destination [%s]", name));
      }
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Using non-JNDI destination [%s], will create one now", name));
    }

    checkArgument(session != null, "Session cannot be null when creating a destination");
    checkArgument(!isBlank(name), "Destination name cannot be blank when creating a destination");

    if (topic) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Creating Topic Destination with name: [%s]", name));
      }

      return session.createTopic(name);
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Creating Queue Destination with name: [%s]", name));
      }
      return session.createQueue(name);
    }
  }

  protected Optional<Destination> createDestinationFromJndi(String name) throws JMSException {
    Optional<Destination> dest = getJndiDestination(name);
    if (LOGGER.isDebugEnabled()) {
      String message = dest.isPresent() ? "located in JNDI, will use it now" : "not found using JNDI";
      LOGGER.debug(format("Destination [%s] %s", name, message));
    }

    return dest;
  }

  protected Optional<Destination> getJndiDestination(String name) {
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Looking up %s from JNDI", name));
      }

      return getJndiObjectSupplier().apply(name);

    } catch (Exception e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Failed to look up destination [%s]: ", name), e);
      }

      return empty();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Destination createTemporaryDestination(Session session) throws JMSException {
    checkArgument(session != null, "Session cannot be null when creating a destination");

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Creating temporary destination"));
    }

    return session.createTemporaryQueue();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send(MessageProducer producer, Message message, boolean persistent, int priority,
                   long ttl, boolean topic)
      throws JMSException {
    if (LOGGER.isDebugEnabled()) {
      Destination dest = producer.getDestination();
      LOGGER.debug(format("Sending message to [%s], persistent:[%s], with priority:[%s] and ttl:[%s]",
                          dest instanceof Queue ? ((Queue) dest).getQueueName() : ((Topic) dest).getTopicName(),
                          persistent, priority, ttl));
    }
    producer.send(message, (persistent ? PERSISTENT : NON_PERSISTENT), priority, ttl);
  }

  private Function<String, Optional<Destination>> getJndiObjectSupplier() {
    return jndiObjectSupplier;
  }

  protected LookupJndiDestination getLookupJndiDestination() {
    return lookupJndiDestination;
  }
}
