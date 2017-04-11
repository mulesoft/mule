/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.publish;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.extensions.jms.api.connection.JmsSpecification.JMS_2_0;
import static org.mule.extensions.jms.internal.common.JmsCommons.resolveOverride;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.config.JmsProducerConfig;
import org.mule.extensions.jms.api.publish.JmsPublishParameters;
import org.mule.extensions.jms.internal.support.JmsSupport;

import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;

/**
 * Wrapper implementation of a JMS {@link MessageProducer}
 *
 * @since 4.0
 */
public final class JmsMessageProducer implements AutoCloseable {

  private static final Logger LOGGER = getLogger(JmsMessageProducer.class);

  private final MessageProducer producer;
  private final JmsSupport jmsSupport;
  private final boolean isTopic;

  public JmsMessageProducer(JmsSupport jmsSupport, MessageProducer producer, boolean isTopic) {
    checkArgument(jmsSupport != null, "A non null JmsSupport implementation is required for publishing");
    checkArgument(producer != null, "A non null MessageProducer is required to use as delegate");

    this.producer = producer;
    this.jmsSupport = jmsSupport;
    this.isTopic = isTopic;
  }

  public void publish(Message message, JmsProducerConfig config, JmsPublishParameters overrides)
      throws JMSException {

    java.util.Optional<Long> delay = resolveDeliveryDelay(config, overrides.getDeliveryDelay(), overrides.getDeliveryDelayUnit());
    Boolean disableMessageId = resolveOverride(config.isDisableMessageId(), overrides.isDisableMessageId());
    Boolean disableMessageTimestamp =
        resolveOverride(config.isDisableMessageTimestamp(), overrides.isDisableMessageTimestamp());
    Boolean persistentDelivery = resolveOverride(config.isPersistentDelivery(), overrides.isPersistentDelivery());
    Integer priority = resolveOverride(config.getPriority(), overrides.getPriority());
    long timeToLive = resolveOverride(config.getTimeToLiveUnit(), overrides.getTimeToLiveUnit())
        .toMillis(resolveOverride(config.getTimeToLive(), overrides.getTimeToLive()));

    configureProducer(delay, disableMessageId, disableMessageTimestamp);

    jmsSupport.send(producer, message, persistentDelivery, priority, timeToLive, isTopic);
  }

  @Override
  public void close() throws JMSException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Closing producer: " + producer);
    }
    producer.close();
  }

  private java.util.Optional<Long> resolveDeliveryDelay(JmsProducerConfig config, Long deliveryDelay, TimeUnit unit) {
    Long delay = resolveOverride(config.getDeliveryDelay(), deliveryDelay);
    TimeUnit delayUnit = resolveOverride(config.getDeliveryDelayUnit(), unit);

    checkArgument(jmsSupport.getSpecification().equals(JMS_2_0) || delay == null,
                  format("[deliveryDelay] is only supported on JMS 2.0 specification,"
                      + " but current configuration is set to JMS %s", jmsSupport.getSpecification().getName()));
    if (delay != null) {
      return of(delayUnit.toMillis(delay));
    }
    return empty();
  }

  private void configureProducer(Optional<Long> deliveryDelay, boolean dissableId, boolean dissableTimeStamp)
      throws JMSException {

    setDisableMessageID(dissableId);
    setDisableMessageTimestamp(dissableTimeStamp);
    deliveryDelay.ifPresent(this::setDeliveryDelay);
  }

  private void setDeliveryDelay(Long value) {
    try {
      producer.setDeliveryDelay(value);
    } catch (JMSException e) {
      LOGGER.error("Failed to configure [setDeliveryDelay] in MessageProducer: ", e);
    }
  }

  private void setDisableMessageID(boolean value) {
    try {
      producer.setDisableMessageID(value);
    } catch (JMSException e) {
      LOGGER.error("Failed to configure [setDisableMessageID] in MessageProducer: ", e);
    }
  }

  private void setDisableMessageTimestamp(boolean value) {
    try {
      producer.setDisableMessageTimestamp(value);
    } catch (JMSException e) {
      LOGGER.error("Failed to configure [setDisableMessageTimestamp] in MessageProducer: ", e);
    }
  }

}
