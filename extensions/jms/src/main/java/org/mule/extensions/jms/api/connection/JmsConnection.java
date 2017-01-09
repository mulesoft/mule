/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.mule.extensions.jms.api.config.AckMode.MANUAL;
import static org.mule.extensions.jms.api.config.AckMode.TRANSACTED;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.api.destination.ConsumerType;
import org.mule.extensions.jms.api.exception.JmsAckException;
import org.mule.extensions.jms.internal.consume.JmsMessageConsumer;
import org.mule.extensions.jms.internal.publish.JmsMessageProducer;
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

/**
 * A Connection for the JmsExtension
 *
 * @since 4.0
 */
public final class JmsConnection implements Stoppable, Disposable {

  private static final Logger LOGGER = getLogger(JmsConnection.class);

  private final JmsSupport jmsSupport;
  private final Connection connection;
  private final Map<String, Message> pendingAckSessions = new HashMap<>();
  private final List<JmsMessageConsumer> createdConsumers = new LinkedList<>();
  private final List<JmsMessageProducer> createdProducers = new LinkedList<>();
  private final List<JmsSession> createdSessions = new LinkedList<>();

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
    JmsSession wrapper;

    if (ackMode.equals(MANUAL)) {
      String ackId = randomAlphanumeric(16);
      pendingAckSessions.put(ackId, null);
      wrapper = new JmsSession(session, ackId);
    } else {
      wrapper = new JmsSession(session);
    }

    createdSessions.add(wrapper);
    return wrapper;
  }

  /**
   * Creates a new JMS {@link MessageConsumer} using the given {@link Session}
   *
   * @param session        the {@link Session} used to create the {@link MessageConsumer}
   * @param jmsDestination the {@link Destination} from which {@link Message}s will be consumed
   * @param selector       a JMS selector string for filtering incoming {@link Message}s. Empty or {@code null} implies no filtering
   * @param consumerType   the {@link ConsumerType} to use based on the {@link Destination} type
   * @return a new {@link MessageConsumer} for the given {@link Destination}
   * @throws JMSException if an error occurs while creating the consumer
   */
  public JmsMessageConsumer createConsumer(Session session, Destination jmsDestination, String selector,
                                           ConsumerType consumerType)
      throws JMSException {
    JmsMessageConsumer consumer = new JmsMessageConsumer(
                                                         jmsSupport.createConsumer(session, jmsDestination, selector,
                                                                                   consumerType));

    createdConsumers.add(consumer);
    return consumer;
  }

  /**
   * Creates a new JMS {@link MessageProducer} using the given {@link Session}
   *
   * @param session        the {@link Session} used to create the {@link MessageProducer}
   * @param jmsDestination the {@link Destination} to where the {@link Message}s will be published
   * @param isTopic        if {@code true} the given {@link Destination} has a {@link Topic} destination type.
   *                       This distinction is made only for {@link JmsSpecification#JMS_1_0_2b} in order to decide whether
   *                       to create a {@link TopicPublisher} or a {@link QueueSender}
   * @return a new {@link MessageProducer} for the given {@link Destination}
   * @throws JMSException if an error occurs while creating the consumer
   */
  public JmsMessageProducer createProducer(Session session, Destination jmsDestination, boolean isTopic) throws JMSException {
    MessageProducer producer = jmsSupport.createProducer(session, jmsDestination, isTopic);
    JmsMessageProducer wrapper = new JmsMessageProducer(jmsSupport, producer, isTopic);
    createdProducers.add(wrapper);
    return wrapper;
  }

  /**
   * Registers the {@link Message} to the {@link Session} using the {@code ackId} in order to being
   * able later to perform a {@link AckMode#MANUAL} ACK
   *
   * @param ackId   the id associated to the {@link Session} used to create the {@link Message}
   * @param message the {@link Message} to use for executing the {@link Message#acknowledge}
   * @throws IllegalArgumentException if no Session was registered with the given AckId
   */
  public void registerMessageForAck(String ackId, Message message) {
    checkArgument(pendingAckSessions.containsKey(ackId),
                  format("Ack pending Messages can only be registered for Sessions created with this Connection, "
                      + "but AckId [%s] was never declared", ackId));

    pendingAckSessions.put(ackId, message);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Registered Message for Session AckId [%s]", ackId));
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
      throw new JmsAckException(format("No pending acknowledgement with ackId [%s] exists in this Connection", ackId));
    }

    message.acknowledge();
  }

  /**
   * Temporarily stops a connection's delivery of incoming messages. Delivery
   * can be restarted using the connection's {@code start} method. When
   * the connection is stopped, delivery to all the connection's message
   * consumers is inhibited.
   *
   */
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

      releaseResources();
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

  /**
   * Release the resources related to the Session, Producers and Consumers that
   * may have been allocated by this Connection.
   * This terminates all message processing on the Sessions created by this Connection.
   */
  public void releaseResources() {
    closeConsumers();
    closeProducers();
    closeSessions();
  }

  private void closeSessions() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Closing Sessions");
    }

    List<JmsSession> closed = createdSessions.stream()
        .filter(session -> !session.getAckId().isPresent() || pendingAckSessions.get(session.getAckId().get()) == null)
        .peek(this::closeQuietly)
        .collect(toList());

    createdSessions.removeAll(closed);
  }

  private void closeConsumers() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Closing Consumers");
    }
    createdConsumers.forEach(this::closeQuietly);
    createdConsumers.clear();
  }

  private void closeProducers() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Closing Producers");
    }
    createdProducers.forEach(this::closeQuietly);
    createdProducers.clear();
  }

  /**
   * Closes {@code this} {@link Connection} resource without throwing an exception (an error message is logged instead)
   *
   * @param closable the resource to close
   */
  private void closeQuietly(AutoCloseable closable) {
    if (closable != null) {
      try {
        closable.close();
      } catch (Exception e) {
        LOGGER.warn("Failed to close jms connection resource: ", e);
      }
    }
  }

}
