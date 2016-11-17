/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.operation;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.extensions.jms.api.config.AckMode.MANUAL;
import static org.mule.extensions.jms.api.config.AckMode.NONE;
import static org.mule.extensions.jms.api.connection.JmsSpecification.JMS_2_0;
import static org.mule.extensions.jms.api.operation.MessageBuilder.BODY_CONTENT_TYPE_JMS_PROPERTY;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.api.config.JmsProducerConfig;
import org.mule.extensions.jms.api.connection.JmsConnection;
import org.mule.extensions.jms.api.connection.JmsSession;
import org.mule.extensions.jms.api.connection.JmsSpecification;
import org.mule.extensions.jms.api.destination.ConsumerType;
import org.mule.extensions.jms.api.destination.QueueConsumer;
import org.mule.extensions.jms.api.destination.TopicConsumer;
import org.mule.extensions.jms.api.exception.JmsTimeoutException;
import org.mule.extensions.jms.internal.support.JmsSupport;

import java.util.concurrent.TimeUnit;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;

/**
 * Utility class for Jms Operations
 *
 * @since 4.0
 */
final class JmsOperationCommons {

  static Message resolveConsumeMessage(MessageConsumer consumer, Long maximumWaitTime) throws JMSException, JmsTimeoutException {
    if (maximumWaitTime == -1) {
      return consumer.receive();
    } else if (maximumWaitTime == 0) {
      return consumer.receiveNoWait();
    } else {
      StopWatch timeoutValidator = new StopWatch();
      timeoutValidator.start();
      Message message = consumer.receive(maximumWaitTime);
      timeoutValidator.stop();

      if (message == null && timeoutValidator.getTime() > maximumWaitTime) {
        throw new JmsTimeoutException(createStaticMessage("Failed to retrieve a Message, operation timed out"));
      }
      return message;
    }
  }

  static String resolveMessageContentType(Message message, String defaultType, Logger logger) {
    try {
      String contentType = message.getStringProperty(BODY_CONTENT_TYPE_JMS_PROPERTY);
      return isBlank(contentType) ? defaultType : contentType;
    } catch (JMSException e) {
      logger.warn(format("Failed to read the Message ContentType from its properties. A default value of [%s] will be used.",
                         defaultType));
      return defaultType;
    }
  }

  static java.util.Optional<Long> resolveDeliveryDelay(JmsSpecification specification, JmsProducerConfig config,
                                                       Long deliveryDelay, TimeUnit unit) {
    Long delay = resolveOverride(config.getDeliveryDelay(), deliveryDelay);
    TimeUnit delayUnit = resolveOverride(config.getDeliveryDelayUnit(), unit);

    checkArgument(specification.equals(JMS_2_0) || delay == null,
                  format("[deliveryDelay] is only supported on JMS 2.0 specification,"
                      + " but current configuration is set to JMS %s", specification.getName()));
    if (delay != null) {
      return of(delayUnit.toMillis(delay));
    }
    return empty();
  }

  static <T> T resolveOverride(T configValue, T operationValue) {
    return operationValue == null ? configValue : operationValue;
  }

  static void evaluateMessageAck(JmsConnection connection, AckMode ackMode, JmsSession session,
                                 Message received, Logger LOGGER)
      throws JMSException {
    if (ackMode.equals(NONE)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Automatically performing an ACK over the message, since AckMode was NONE");
      }
      received.acknowledge();

    } else if (ackMode.equals(MANUAL)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Registering pending ACK on session: " + session.getAckId());
      }
      String id =
          session.getAckId().orElseThrow(() -> new IllegalArgumentException("An AckId is required when MANUAL AckMode is set"));

      connection.registerMessageForAck(id, received);
    }
  }

  static ConsumerType setReplyDestination(MessageBuilder messageBuilder, JmsSession session,
                                          JmsSupport jmsSupport, Message message, Logger logger)
      throws JMSException {

    if (message.getJMSReplyTo() != null) {
      return messageBuilder.getReplyTo().getDestinationType().isTopic() ? new TopicConsumer() : new QueueConsumer();
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Using temporary destination");
      }
      message.setJMSReplyTo(jmsSupport.createTemporaryDestination(session.get()));
      return new QueueConsumer();
    }
  }

  static MessageProducer createProducer(JmsConnection connection, JmsProducerConfig config, boolean isTopic,
                                        Session session, java.util.Optional<Long> deliveryDelay,
                                        Destination jmsDestination, Logger logger)
      throws JMSException {

    MessageProducer producer = connection.createProducer(session, jmsDestination, isTopic);
    setDisableMessageID(producer, config.isDisableMessageId(), logger);
    setDisableMessageTimestamp(producer, config.isDisableMessageTimestamp(), logger);
    if (deliveryDelay.isPresent()) {
      setDeliveryDelay(producer, deliveryDelay.get(), logger);
    }

    return producer;
  }

  static void setDeliveryDelay(MessageProducer producer, Long value, Logger logger) {
    try {
      producer.setDeliveryDelay(value);
    } catch (JMSException e) {
      logger.error("Failed to configure [setDeliveryDelay] in MessageProducer: ", e);
    }
  }

  static void setDisableMessageID(MessageProducer producer, boolean value, Logger logger) {
    try {
      producer.setDisableMessageID(value);
    } catch (JMSException e) {
      logger.error("Failed to configure [setDisableMessageID] in MessageProducer: ", e);
    }
  }

  static void setDisableMessageTimestamp(MessageProducer producer, boolean value, Logger logger) {
    try {
      producer.setDisableMessageTimestamp(value);
    } catch (JMSException e) {
      logger.error("Failed to configure [setDisableMessageTimestamp] in MessageProducer: ", e);
    }
  }
}
