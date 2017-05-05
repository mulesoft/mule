/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.operation;

import static java.lang.String.format;
import static org.mule.extensions.jms.internal.common.JmsCommons.evaluateMessageAck;
import static org.mule.extensions.jms.internal.common.JmsCommons.resolveMessageContentType;
import static org.mule.extensions.jms.internal.common.JmsCommons.resolveMessageEncoding;
import static org.mule.extensions.jms.internal.common.JmsCommons.resolveOverride;
import static org.mule.extensions.jms.internal.common.JmsCommons.toInternalAckMode;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.config.JmsProducerConfig;
import org.mule.extensions.jms.api.destination.ConsumerType;
import org.mule.extensions.jms.api.destination.QueueConsumer;
import org.mule.extensions.jms.api.destination.TopicConsumer;
import org.mule.extensions.jms.api.exception.JmsConsumeException;
import org.mule.extensions.jms.api.exception.JmsExtensionException;
import org.mule.extensions.jms.api.exception.JmsPublishConsumeErrorTypeProvider;
import org.mule.extensions.jms.api.exception.JmsPublishException;
import org.mule.extensions.jms.api.message.JmsAttributes;
import org.mule.extensions.jms.api.message.JmsMessageBuilder;
import org.mule.extensions.jms.api.publish.JmsPublishParameters;
import org.mule.extensions.jms.consume.JmsConsumeParameters;
import org.mule.extensions.jms.internal.config.InternalAckMode;
import org.mule.extensions.jms.internal.config.JmsConfig;
import org.mule.extensions.jms.internal.connection.JmsConnection;
import org.mule.extensions.jms.internal.connection.JmsSession;
import org.mule.extensions.jms.internal.connection.session.JmsSessionManager;
import org.mule.extensions.jms.internal.consume.JmsMessageConsumer;
import org.mule.extensions.jms.internal.message.JmsResultFactory;
import org.mule.extensions.jms.internal.metadata.JmsOutputResolver;
import org.mule.extensions.jms.internal.support.JmsSupport;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;

import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;

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

  @Inject
  private JmsSessionManager sessionManager;

  /**
   * Operation that allows the user to send a message to a JMS {@link Destination} and waits for a response
   * either to the provided {@code ReplyTo} destination or to a temporary {@link Destination} created dynamically
   *
   * @param config          the current {@link JmsProducerConfig}
   * @param connection      the current {@link JmsConnection}
   * @param destination     the name of the {@link Destination} where the {@link Message} should be sent
   * @param messageBuilder  the {@link JmsMessageBuilder} used to create the {@link Message} to be sent
   * @param publishParameters       Parameter group that lets override the publish configuration
   * @param consumeParameters       Parameter group that lets override the consume configuration
   * @return a {@link Result} with the reply {@link Message} content as {@link Result#getOutput} and its properties
   * and headers as {@link Result#getAttributes}
   * @throws JmsExtensionException if an error occurs
   */
  @OutputResolver(output = JmsOutputResolver.class)
  @Throws(JmsPublishConsumeErrorTypeProvider.class)
  public Result<Object, JmsAttributes> publishConsume(@Config JmsConfig config,
                                                      @Connection JmsConnection connection,
                                                      @Placement(order = 0) @XmlHints(
                                                          allowReferences = false) @Summary("The name of the Queue destination where the Message should be sent") String destination,
                                                      @Placement(
                                                          order = 1) @Summary("A builder for the message that will be published") @ParameterGroup(
                                                              name = "Message",
                                                              showInDsl = true) JmsMessageBuilder messageBuilder,
                                                      @Placement(order = 2) @ParameterGroup(
                                                          name = "Publish Configuration",
                                                          showInDsl = true) JmsPublishParameters publishParameters,
                                                      @Placement(order = 3) @ParameterGroup(
                                                          name = "Consume Configuration",
                                                          showInDsl = true) JmsConsumeParameters consumeParameters)
      throws JmsExtensionException {

    JmsSession session;
    Message message;
    ConsumerType replyConsumerType;
    InternalAckMode resolvedAckMode = resolveOverride(
                                                      toInternalAckMode(config.getConsumerConfig().getAckMode()),
                                                      toInternalAckMode(consumeParameters.getAckMode()));

    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Begin publish");
      }

      JmsSupport jmsSupport = connection.getJmsSupport();
      session = connection.createSession(resolvedAckMode, false);

      message = messageBuilder.build(jmsSupport, session.get(), config);
      replyConsumerType = setReplyDestination(messageBuilder, session, jmsSupport, message);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Message built, sending message to [" + destination + "]");
      }

      Destination jmsDestination = jmsSupport.createDestination(session.get(), destination, false);
      connection.createProducer(session.get(), jmsDestination, false)
          .publish(message, config.getProducerConfig(), publishParameters);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Message sent to destination [%s], prepare for response.", destination));
      }
    } catch (Exception e) {
      String msg = format("An error occurred while sending a message to destination [%s] of type QUEUE: ", destination);
      LOGGER.error(msg, e);
      throw new JmsPublishException(msg, e);
    }

    try {
      JmsMessageConsumer consumer = connection.createConsumer(session.get(), message.getJMSReplyTo(), "", replyConsumerType);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Waiting for incoming message in destination [%s]",
                            getReplyDestinationName(message.getJMSReplyTo(), replyConsumerType)));
      }

      Message received = consumer.consume(consumeParameters.getMaximumWaitUnit().toMillis(consumeParameters.getMaximumWait()));

      if (received != null) {
        evaluateMessageAck(resolvedAckMode, session, received, sessionManager, null);
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Creating response result");
      }

      return resultFactory.createResult(received, connection.getJmsSupport().getSpecification(),
                                        resolveOverride(resolveMessageContentType(received, config.getContentType()),
                                                        consumeParameters.getInboundContentType()),
                                        resolveOverride(resolveMessageEncoding(received, config.getEncoding()),
                                                        consumeParameters.getInboundEncoding()),
                                        session.getAckId());
    } catch (Exception e) {
      LOGGER.error("An error occurred while listening for the reply: ", e);
      throw new JmsConsumeException("An error occurred while listening for the reply: ", e);
    }
  }

  private ConsumerType setReplyDestination(JmsMessageBuilder messageBuilder, JmsSession session,
                                           JmsSupport jmsSupport, Message message)
      throws JMSException {

    if (message.getJMSReplyTo() != null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Using provided destination: [%s]", messageBuilder.getReplyTo().getDestination()));
      }

      return messageBuilder.getReplyTo().getDestinationType().isTopic() ? new TopicConsumer() : new QueueConsumer();
    } else {
      Destination temporaryDestination = jmsSupport.createTemporaryDestination(session.get());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Using temporary destination: [%s]", ((Queue) temporaryDestination).getQueueName()));
      }
      message.setJMSReplyTo(temporaryDestination);
      return new QueueConsumer();
    }
  }

  private String getReplyDestinationName(Destination destination, ConsumerType replyConsumerType) throws JMSException {
    return replyConsumerType.topic() ? ((Topic) destination).getTopicName() : ((Queue) destination).getQueueName();
  }
}
