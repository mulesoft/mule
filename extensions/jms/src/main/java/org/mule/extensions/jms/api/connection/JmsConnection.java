/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection;

import static java.lang.String.format;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.extensions.jms.api.config.AckMode.MANUAL;
import static org.mule.extensions.jms.api.config.AckMode.TRANSACTED;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.api.destination.ConsumerType;
import org.mule.extensions.jms.internal.support.JmsSupport;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Stoppable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.QueueSender;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Connection for the JmsExtension
 *
 * @since 4.0
 */
public final class JmsConnection implements Stoppable, Disposable {

  private static final Logger LOGGER = LoggerFactory.getLogger(JmsConnection.class);

  private final JmsSupport jmsSupport;
  private final Connection connection;
  private final Map<String, Message> pendingAckSessions = new HashMap<>();
  private final List<MessageConsumer> createdConsumers = new LinkedList<>();
  private final List<MessageProducer> createdProducers = new LinkedList<>();
  private final List<Session> createdSessions = new LinkedList<>();

  public JmsConnection(JmsSupport jmsSupport, Connection connection) {
    this.jmsSupport = jmsSupport;
    this.connection = connection;
  }

  public JmsSupport getJmsSupport() {
    return jmsSupport;
  }

  public Connection get() {
    return connection;
  }

  /**
   * Creates a new JMS {@link Session} using the current {@link Connection}
   *
   * @param ackMode the {@link Session} {@link AckMode}
   * @param isTopic if {@code true} the {@link Session} created will be a {@link TopicSession}.
   *                This distinction is made only for {@link JmsSpecification#JMS_1_0_2b}
   * @return a new {@link Session}
   * @throws JMSException if an error occurs while creating the {@link Session}
   */
  public JmsSession createSession(AckMode ackMode, boolean isTopic) throws JMSException {
    Session session = jmsSupport.createSession(connection, isTopic, ackMode.equals(TRANSACTED), ackMode.getAckMode());
    createdSessions.add(session);

    if (ackMode.equals(MANUAL)) {
      String ackId = randomAlphanumeric(16);
      pendingAckSessions.put(ackId, null);
      return new JmsSession(session, ackId);
    }

    return new JmsSession(session);
  }

  /**
   * Creates a new JMS {@link MessageConsumer} using the given {@link Session}
   *
   * @param session the {@link Session} used to create the {@link MessageConsumer}
   * @param jmsDestination the {@link Destination} from which {@link Message}s will be consumed
   * @param selector a JMS selector string for filtering incoming {@link Message}s. Empty or {@code null} implies no filtering
   * @param consumerType the {@link ConsumerType} to use based on the {@link Destination} type
   * @return a new {@link MessageConsumer} for the given {@link Destination}
   * @throws JMSException if an error occurs while creating the consumer
   */
  public MessageConsumer createConsumer(Session session, Destination jmsDestination, String selector, ConsumerType consumerType)
      throws JMSException {
    MessageConsumer consumer = jmsSupport.createConsumer(session, jmsDestination, selector, consumerType);
    createdConsumers.add(consumer);
    return consumer;
  }

  /**
   * Creates a new JMS {@link MessageProducer} using the given {@link Session}
   *
   * @param session the {@link Session} used to create the {@link MessageProducer}
   * @param jmsDestination the {@link Destination} to where the {@link Message}s will be published
   * @param isTopic if {@code true} the given {@link Destination} has a {@link Topic} destination type.
   *                This distinction is made only for {@link JmsSpecification#JMS_1_0_2b} in order to decide whether
   *                to create a {@link TopicPublisher} or a {@link QueueSender}
   * @return a new {@link MessageProducer} for the given {@link Destination}
   * @throws JMSException if an error occurs while creating the consumer
   */
  public MessageProducer createProducer(Session session, Destination jmsDestination, boolean isTopic) throws JMSException {
    MessageProducer producer = jmsSupport.createProducer(session, jmsDestination, isTopic);
    createdProducers.add(producer);
    return producer;
  }

  /**
   * Registers the {@link Message} to the {@link Session} using the {@code ackId} in order to being
   * able later to perform a {@link AckMode#MANUAL} ACK
   *
   * @param ackId the id associated to the {@link Session} used to create the {@link Message}
   * @param message the {@link Message} to use for executing the {@link Message#acknowledge}
   */
  public void registerMessageForAck(String ackId, Message message) {
    if (!isBlank(ackId) && pendingAckSessions.get(ackId) == null) {
      pendingAckSessions.put(ackId, message);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Registered Message for Session AckId [%s]", ackId));
      }
    }
  }

  /**
   * Executes the {@link Message#acknowledge} on the latest {@link Message} associated to the {@link Session}
   * identified by the {@code ackId}
   *
   * @param ackId the id associated to the {@link Session} that should be ACKed
   * @throws JMSException if an error occurs during the ack
   */
  public void doAck(String ackId) throws JMSException {

    Message message = pendingAckSessions.get(ackId);
    if (message == null) {
      throw new IllegalArgumentException(format("No pending acknowledgement with ackId [%s] exists in this Connection", ackId));
    }

    message.acknowledge();
  }

  @Override
  public void stop() throws MuleException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Stopping JMS Connection: " + connection);
    }
    try {
      connection.stop();
    } catch (javax.jms.IllegalStateException ex) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Ignoring Connection state exception - assuming already closed: ", ex);
      }
    } catch (JMSException ex) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Could not stop JMS Connection - assuming this method has been called in a Java EE web or EJB application: ",
                     ex);
      }
    }
  }

  @Override
  public void dispose() {
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Closing JMS Connection: " + connection);
      }

      closeConsumers();
      closeProducers();
      closeSessions();
      connection.close();
      pendingAckSessions.clear();
    } catch (javax.jms.IllegalStateException ex) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Ignoring Connection state exception - assuming already closed: ", ex);
      }
    } catch (JMSException ex) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Could not close JMS Connection : ", ex);
      }
    }
  }

  private void closeSessions() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Closing Session");
    }
    createdSessions.forEach(this::closeQuietly);
  }

  private void closeConsumers() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Closing Consumers");
    }
    createdConsumers.forEach(this::closeQuietly);
  }

  private void closeProducers() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Closing Producers");
    }
    createdProducers.forEach(this::closeQuietly);
  }

  /**
   * Closes the MessageConsumer
   *
   * @param consumer {@link MessageConsumer} to close
   * @throws JMSException if an error occurs
   */
  private void close(MessageConsumer consumer) throws JMSException {
    if (consumer != null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Closing consumer: " + consumer);
      }
      consumer.close();
    } else if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Consumer is null, nothing to close");
    }
  }

  /**
   * Closes the {@link MessageConsumer} without throwing an exception (an error message is logged instead)
   *
   * @param consumer the {@link MessageConsumer} to close
   */
  private void closeQuietly(MessageConsumer consumer) {
    try {
      close(consumer);
    } catch (Exception e) {
      LOGGER.warn("Failed to close jms message consumer: " + e.getMessage());
    }
  }

  /**
   * Closes the {@link Session}
   *
   * @param session the {@link Session} to close
   * @throws JMSException
   */
  private void close(Session session) throws JMSException {
    if (session != null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Closing session " + session);
      }
      session.close();
    }
  }

  /**
   * Closes the {@link Session} without throwing an exception (an error message is logged instead)
   *
   * @param session the {@link Session} to close
   */
  private void closeQuietly(Session session) {
    if (session != null) {
      try {
        close(session);
      } catch (Exception e) {
        LOGGER.warn("Failed to close jms session: " + e.getMessage());
      }
    }
  }

  /**
   * Closes the {@link MessageProducer}
   *
   * @param producer the {@link MessageProducer} to close
   * @throws JMSException
   */
  private void close(MessageProducer producer) throws JMSException {
    if (producer != null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Closing producer: " + producer);
      }
      producer.close();
    } else if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Producer is null, nothing to close");
    }
  }

  /**
   * Closes the {@link MessageProducer} without throwing an exception (an error message is logged instead)
   *
   * @param producer the {@link MessageProducer} to close
   */
  private void closeQuietly(MessageProducer producer) {
    try {
      close(producer);
    } catch (Exception e) {
      LOGGER.warn("Failed to close jms message producer: " + e.getMessage());
    }
  }

}
