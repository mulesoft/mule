/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.config;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extensions.jms.api.connection.JmsSpecification;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.concurrent.TimeUnit;

import javax.jms.DeliveryMode;
import javax.jms.Message;

/**
 * Configuration parameters for sending messages to a JMS Queue or Topic
 *
 * @since 4.0
 */
@XmlHints(allowTopLevelDefinition = true)
public class JmsProducerConfig {

  /**
   * Sets the default value for the {@link Message#getJMSDeliveryMode}.
   * If {@code true}, the {@link DeliveryMode#PERSISTENT} mode will be used,
   * which instructs the JMS provider to take extra care to insure the message
   * is not lost in transit due to a JMS provider failure.
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Expression(NOT_SUPPORTED)
  @Summary("If true, the Message will be sent using the PERSISTENT JMSDeliveryMode")
  private boolean persistentDelivery;

  /**
   * Defines the default {@link Message#getJMSPriority} to use when sending messages
   */
  @Parameter
  @Optional(defaultValue = "4")
  @Expression(NOT_SUPPORTED)
  @Summary("The default JMSPriority value to be used when sending the message")
  private int priority;

  /**
   * Defines the default time the message will be in the broker before it expires and is discarded
   */
  @Parameter
  @Optional(defaultValue = "0")
  @Expression(NOT_SUPPORTED)
  @Summary("Defines the default time the message will be in the broker before it expires and is discarded")
  //TODO MULE-11091: Bundle with unit
  private long timeToLive;

  /**
   * A {@link TimeUnit} which qualifies the {@link #timeToLive} attribute.
   * <p>
   * Defaults to {@code MILLISECONDS}
   */
  @Parameter
  @Optional(defaultValue = "MILLISECONDS")
  @Expression(NOT_SUPPORTED)
  @Summary("Time unit to be used in the timeToLive configurations")
  //TODO MULE-11091: Bundle with unit
  private TimeUnit timeToLiveUnit;

  /**
   * Defines the default value to use when producing messages,
   * for disabling the {@link Message#getJMSMessageID} generation in the broker.
   * Depending on the provider it may or may not have effect
   */
  @Parameter
  @Optional(defaultValue = "false")
  @Expression(NOT_SUPPORTED)
  @Summary("If true, the Message will be flagged to avoid generating its MessageID")
  private boolean disableMessageId;

  /**
   * Defines the default value to use, when producing messages,
   * for disable {@link Message#getJMSTimestamp} generation in the broker.
   * Depending on the provider it may or may not have effect.
   */
  @Parameter
  @Optional(defaultValue = "false")
  @Expression(NOT_SUPPORTED)
  @Summary("If true, the Message will be flagged to avoid generating its sent Timestamp")
  private boolean disableMessageTimestamp;

  /**
   * This is used to determine the {@link Message} delivery delay time which is
   * calculated by adding the {@code deliveryDelay} value specified on the
   * send method to the time the message was sent.
   * <p>
   * Only used in {@link JmsSpecification#JMS_2_0}
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  @Optional
  @Summary("Only used by JMS 2.0:  Sets the delivery delay to be applied in order to postpone the Message delivery")
  //TODO MULE-11091: Bundle with unit
  private Long deliveryDelay;

  /**
   * A {@link TimeUnit} which qualifies the {@link #deliveryDelay} attribute.
   * <p>
   * Defaults to {@code MILLISECONDS}
   */
  @Parameter
  @Optional(defaultValue = "MILLISECONDS")
  @Expression(NOT_SUPPORTED)
  @Summary("Only used by JMS 2.0: Time unit to be used in the deliveryDelay configurations.")
  //TODO MULE-11091: Bundle with unit
  private TimeUnit deliveryDelayUnit;

  /**
   * A message JMSType identifier supplied by a client when a message is sent.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  @Summary("The message type identifier to be set in the JMSType Header when a message is sent")
  private String jmsType;

  public boolean isPersistentDelivery() {
    return persistentDelivery;
  }

  public int getPriority() {
    return priority;
  }

  public long getTimeToLive() {
    return timeToLive;
  }

  public TimeUnit getTimeToLiveUnit() {
    return timeToLiveUnit;
  }

  public boolean isDisableMessageId() {
    return disableMessageId;
  }

  public boolean isDisableMessageTimestamp() {
    return disableMessageTimestamp;
  }

  public Long getDeliveryDelay() {
    return deliveryDelay;
  }

  public TimeUnit getDeliveryDelayUnit() {
    return deliveryDelayUnit;
  }

  public String getJmsType() {
    return jmsType;
  }

}
