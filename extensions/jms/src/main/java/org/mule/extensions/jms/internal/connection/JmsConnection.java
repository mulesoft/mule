/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.connection;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.mule.extensions.jms.internal.config.InternalAckMode.MANUAL;
import static org.mule.extensions.jms.internal.config.InternalAckMode.TRANSACTED;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.connection.JmsSpecification;
import org.mule.extensions.jms.internal.config.InternalAckMode;
import org.mule.extensions.jms.api.destination.ConsumerType;
import org.mule.extensions.jms.internal.connection.session.JmsSessionManager;
import org.mule.extensions.jms.internal.consume.JmsMessageConsumer;
import org.mule.extensions.jms.internal.publish.JmsMessageProducer;
import org.mule.extensions.jms.internal.support.JmsSupport;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Stoppable;

import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.List;

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

/**
 * A Connection for the JmsExtension
 *
 * @since 4.0
 */
public class JmsConnection implements Stoppable, Disposable {

  private static final Logger LOGGER = getLogger(JmsConnection.class);

  private final JmsSupport jmsSupport;
  private final Connection connection;
  final JmsSessionManager jmsSessionManager;
  private final List<JmsMessageConsumer> createdConsumers = new LinkedList<>();
  private final List<JmsMessageProducer> createdProducers = new LinkedList<>();
  private final List<JmsSession> createdSessions = new LinkedList<>();

  public JmsConnection(JmsSupport jmsSupport, Connection connection, JmsSessionManager jmsSessionManager) {
    this.jmsSupport = jmsSupport;
    this.connection = connection;
    this.jmsSessionManager = jmsSessionManager;
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
   * @param ackMode the {@link Session} {@link InternalAckMode}
   * @param isTopic if {@code true} the {@link Session} created will be a {@link TopicSession}.
   *                This distinction is made only for {@link JmsSpecification#JMS_1_0_2b}
   * @return a new {@link Session}
   * @throws JMSException if an error occurs while creating the {@link Session}
   */
  public JmsSession createSession(InternalAckMode ackMode, boolean isTopic) throws JMSException {
    Session session = jmsSupport.createSession(connection, isTopic, ackMode.equals(TRANSACTED), ackMode.getAckModeValue());
    JmsSession wrapper;

    if (ackMode.equals(MANUAL)) {
      String ackId = randomAlphanumeric(16);
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
        .filter(session -> !session.getAckId().isPresent())
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
