/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.operation;

import static org.mule.extensions.jms.api.config.AckMode.AUTO;
import static org.mule.extensions.jms.api.operation.JmsOperationCommons.createProducer;
import static org.mule.extensions.jms.api.operation.JmsOperationCommons.evaluateMessageAck;
import static org.mule.extensions.jms.api.operation.JmsOperationCommons.resolveConsumeMessage;
import static org.mule.extensions.jms.api.operation.JmsOperationCommons.resolveDeliveryDelay;
import static org.mule.extensions.jms.api.operation.JmsOperationCommons.resolveMessageContentType;
import static org.mule.extensions.jms.api.operation.JmsOperationCommons.resolveOverride;
import static org.mule.extensions.jms.api.operation.JmsOperationCommons.setReplyDestination;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.api.config.JmsConfig;
import org.mule.extensions.jms.api.config.JmsProducerConfig;
import org.mule.extensions.jms.api.connection.JmsConnection;
import org.mule.extensions.jms.api.connection.JmsSession;
import org.mule.extensions.jms.api.destination.ConsumerType;
import org.mule.extensions.jms.api.exception.JmsExtensionException;
import org.mule.extensions.jms.api.message.JmsAttributes;
import org.mule.extensions.jms.internal.message.JmsResultFactory;
import org.mule.extensions.jms.internal.metadata.JmsOutputResolver;
import org.mule.extensions.jms.internal.support.JmsSupport;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.concurrent.TimeUnit;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;

import org.slf4j.Logger;

/**
 * Operation that allows the user to send a message to a JMS {@link Destination} and waits for a response
 * either to the provided {@code ReplyTo} destination or to a temporary {@link Destination} created dynamically
 *
 * @since 4.0
 */
public class JmsPublishConsume {

  private static final Logger LOGGER = getLogger(JmsPublishConsume.class);
  private JmsResultFactory resultFactory = new JmsResultFactory();

  /**
   * Operation that allows the user to send a message to a JMS {@link Destination} and waits for a response
   * either to the provided {@code ReplyTo} destination or to a temporary {@link Destination} created dynamically
   *
   * @param config             the current {@link JmsProducerConfig}
   * @param connection         the current {@link JmsConnection}
   * @param destination        the name of the {@link Destination} where the {@link Message} should be sent
   * @param messageBuilder     the {@link MessageBuilder} used to create the {@link Message} to be sent
   * @param messageBuilder     the {@link MessageBuilder} used to create the {@link Message} to be sent
   * @param persistentDelivery {@code true} if {@link DeliveryMode#PERSISTENT} should be used
   * @param priority           the {@link Message#getJMSPriority} to be set
   * @param timeToLive         the time the message will be in the broker before it expires and is discarded
   * @param timeToLiveUnit     unit to be used in the timeToLive configurations
   * @param ackMode            the {@link AckMode} that will be configured over the Message and Session
   * @param deliveryDelay      Only used by JMS 2.0. Sets the delivery delay to be applied in order to postpone the Message delivery
   * @param deliveryDelayUnit  Time unit to be used in the deliveryDelay configurations
   * @return a {@link Result} with the reply {@link Message} content as {@link Result#getOutput} and its properties
   * and headers as {@link Result#getAttributes}
   * @throws JmsExtensionException if an error occurs
   */
  @OutputResolver(output = JmsOutputResolver.class)
  public Result<Object, JmsAttributes> publishConsume(@UseConfig JmsConfig config, @Connection JmsConnection connection,
                                                      @XmlHints(
                                                          allowReferences = false) @Summary("The name of the Destination where the Message should be sent") String destination,
                                                      @Optional @NullSafe @Summary("A builder for the message that will be published") MessageBuilder messageBuilder,
                                                      @Optional @Summary("If true, the Message will be sent using the PERSISTENT JMSDeliveryMode") Boolean persistentDelivery,
                                                      @Optional @Summary("The default JMSPriority value to be used when sending the message") Integer priority,
                                                      @Optional @Summary("Defines the default time the message will be in the broker before it expires and is discarded") Long timeToLive,
                                                      @Optional @Summary("Time unit to be used in the timeToLive configuration") TimeUnit timeToLiveUnit,
                                                      @Optional(
                                                          defaultValue = "10000") @Summary("Maximum time to wait for a response before timeout") Long maximumWaitTime,
                                                      @Optional(
                                                          defaultValue = "MILLISECONDS") @Summary("Time unit to be used in the maximumWaitTime configuration") TimeUnit waitTimeUnit,
                                                      @Optional AckMode ackMode,
                                                      // JMS 2.0
                                                      @Optional @Summary("Only used by JMS 2.0. Sets the delivery delay to be applied in order to postpone the Message delivery") Long deliveryDelay,
                                                      @Optional @Summary("Time unit to be used in the deliveryDelay configurations") TimeUnit deliveryDelayUnit)
      throws JmsExtensionException {

    JmsProducerConfig producerConfig = config.getProducerConfig();
    java.util.Optional<Long> delay = resolveDeliveryDelay(connection.getJmsSupport().getSpecification(),
                                                          producerConfig, deliveryDelay, deliveryDelayUnit);
    persistentDelivery = resolveOverride(producerConfig.isPersistentDelivery(), persistentDelivery);
    priority = resolveOverride(producerConfig.getPriority(), priority);
    timeToLive = resolveOverride(producerConfig.getTimeToLiveUnit(), timeToLiveUnit)
        .toMillis(resolveOverride(producerConfig.getTimeToLive(), timeToLive));
    ackMode = resolveOverride(config.getConsumerConfig().getAckMode(), ackMode);

    JmsSession session;
    Message message;
    ConsumerType replyConsumerType;
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Begin publish");
      }

      JmsSupport jmsSupport = connection.getJmsSupport();
      session = connection.createSession(AUTO, false);

      message = messageBuilder.build(jmsSupport, session.get(), config);
      replyConsumerType = setReplyDestination(messageBuilder, session, jmsSupport, message, LOGGER);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Message built, sending message");
      }

      Destination jmsDestination = jmsSupport.createDestination(session.get(), destination, false);
      MessageProducer producer = createProducer(connection, producerConfig, false, session.get(),
                                                delay, jmsDestination, LOGGER);
      jmsSupport.send(producer, message, jmsDestination, persistentDelivery, priority, timeToLive, false);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Message Sent, prepare for response");
      }
    } catch (Exception e) {
      LOGGER.error("An error occurred while sending a message: ", e);
      throw new JmsExtensionException(createStaticMessage("An error occurred while sending a message to %s: ", destination), e);
    }

    try {
      MessageConsumer consumer = connection.createConsumer(session.get(), message.getJMSReplyTo(), "", replyConsumerType);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Waiting for incoming message");
      }

      Message received = resolveConsumeMessage(consumer, waitTimeUnit.toMillis(maximumWaitTime));

      if (received != null) {
        evaluateMessageAck(connection, ackMode, session, received, LOGGER);
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Creating response result");
      }

      return resultFactory.createResult(received, connection.getJmsSupport().getSpecification(),
                                        resolveMessageContentType(message, config.getContentType(), LOGGER),
                                        config.getEncoding(),
                                        session.getAckId());
    } catch (Exception e) {
      LOGGER.error("An error occurred while listening for the reply: ", e);
      throw new JmsExtensionException(createStaticMessage("An error occurred while listening for the reply: "), e);
    }
  }

}
