/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.operation;

import static java.lang.String.format;
import static org.mule.extensions.jms.api.config.AckMode.AUTO;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.config.JmsConfig;
import org.mule.extensions.jms.api.config.JmsProducerConfig;
import org.mule.extensions.jms.api.connection.JmsConnection;
import org.mule.extensions.jms.api.connection.JmsSession;
import org.mule.extensions.jms.api.destination.DestinationType;
import org.mule.extensions.jms.api.exception.JmsExtensionException;
import org.mule.extensions.jms.api.exception.JmsPublishException;
import org.mule.extensions.jms.api.exception.JmsPublisherErrorTypeProvider;
import org.mule.extensions.jms.api.message.MessageBuilder;
import org.mule.extensions.jms.api.publish.JmsPublishParameters;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.slf4j.Logger;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;

/**
 * Operation that allows the user to send a message to a JMS {@link Destination}
 *
 * @since 4.0
 */
public final class JmsPublish {

  private static final Logger LOGGER = getLogger(JmsPublish.class);

  /**
   * Operation that allows the user to send a {@link Message} to a JMS {@link Destination
   *
   * @param config             the current {@link JmsProducerConfig }
   * @param connection         the current {@link JmsConnection}
   * @param destination        the name of the {@link Destination} where the {@link Message} should be sent
   * @param type               the {@link DestinationType} of the {@code destination}
   * @param messageBuilder     the {@link MessageBuilder } used to create the {@link Message} to be sent
   * @param persistentDelivery {@code true} if {@link DeliveryMode#PERSISTENT} should be used
   * @param priority           the {@link Message#getJMSPriority} to be set
   * @param timeToLive         the time the message will be in the broker before it expires and is discarded
   * @param timeToLiveUnit     unit to be used in the timeToLive configurations
   * @param deliveryDelay      Only used by JMS 2.0. Sets the delivery delay to be applied in order to postpone the Message delivery
   * @param deliveryDelayUnit  Time unit to be used in the deliveryDelay configurations
   * @throws JmsPublishException if an error occurs
   */
  @Throws(JmsPublisherErrorTypeProvider.class)
  public void publish(@UseConfig JmsConfig config, @Connection JmsConnection connection,
                      @XmlHints(
                          allowReferences = false) @Summary("The name of the Destination where the Message should be sent") String destination,
                      @Optional(defaultValue = "QUEUE") @Summary("The type of the Destination") DestinationType destinationType,
                      @Optional @NullSafe @Summary("A builder for the message that will be published") MessageBuilder messageBuilder,
                      @ParameterGroup(name = "Publish Configuration") JmsPublishParameters overrides)

      throws JmsExtensionException {

    JmsProducerConfig producerConfig = config.getProducerConfig();
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Begin publish");
      }

      JmsSession session = connection.createSession(AUTO, destinationType.isTopic());
      Message message = messageBuilder.build(connection.getJmsSupport(), session.get(), config);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Message built, sending message to [%s] using session [%s]", destination, session.get()));
      }

      Destination jmsDestination = connection.getJmsSupport()
          .createDestination(session.get(), destination, destinationType.isTopic());

      connection.createProducer(session.get(), jmsDestination, destinationType.isTopic())
          .publish(message, producerConfig, overrides);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Finished publish");
      }
    } catch (Exception e) {
      LOGGER.error(format("An error occurred while sending a message to [%s]: ", destination), e);
      throw new JmsPublishException(format("An error occurred while sending a message to [%s]: ", destination), e);
    }
  }

}
