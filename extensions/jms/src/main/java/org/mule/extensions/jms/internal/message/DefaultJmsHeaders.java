/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.message;


import org.mule.extensions.jms.api.destination.JmsDestination;
import org.mule.extensions.jms.api.message.JmsHeaders;

import javax.jms.DeliveryMode;

final class DefaultJmsHeaders implements JmsHeaders {

  private DefaultJmsHeaders() {}

  /**
   * the destination to which the message is being sent
   */
  private JmsDestination destination;

  /**
   * the delivery mode specified when the message was sent, which
   * can be either {@link DeliveryMode#PERSISTENT} or {@link DeliveryMode#NON_PERSISTENT}
   */
  private Integer deliveryMode;

  /**
   * JMS provider calculates its expiration time by adding the {@code timeToLive}
   * value specified on the send method to the time the message was sent (for transacted sends,
   * this is the time the client sends the message, not the time the transaction is committed)
   * <p>
   * If the {@code timeToLive} is specified as {@code zero}, the message's expiration time is
   * set to zero to indicate that the message does not expire.
   *
   * the message's expiration time or {@code zero} if the message does not expire
   */
  private Long expiration;

  /**
   * JMS defines a ten level priority value with 0 as the lowest priority and 9 as the highest.
   * In addition, clients should consider priorities 0-4 as gradations of {@code normal} priority
   * and priorities 5-9 as gradations of {@code expedited} priority.
   * <p>
   * JMS does not require that a provider strictly implement priority ordering of messages;
   * however, it should do its best to deliver expedited messages ahead of normal messages.
   *
   * the message priority level.
   */
  private Integer priority;

  /**
   * a value that uniquely identifies each message sent by a provider.
   * If the Producer was hinted to {@code disableMessageID}, then {@code null}
   * is returned.
   */
  private String messageId;

  /**
   * he time a message was handed off to a provider to be sent.
   * It is not the time the message was actually transmitted because the actual send
   * may occur later due to transactions or other client side queueing of messages.
   * <p>
   * If the Producer was hinted to {@code disableMessageTimestamp}, then {@code zero}
   * is returned.
   */
  private Long timestamp;

  /**
   * Used to link one message with another. A typical use is to link a response
   * message with its request message, using its messageID
   *
   * the message correlationId
   */
  private String correlactionId;

  /**
   * the name of the Destination supplied by a client when a message is sent,
   * where a reply to the message should be sent.
   * If no {@code replyTo} destination was set, then {@code null} is returned.
   */
  private JmsDestination replyTo;

  /**
   * a message type identifier supplied by a client when a message is sent.
   */
  private String type;

  /**
   * If {@code true}, it is likely, but not guaranteed,
   * that this message was delivered but not acknowledged in the past.
   * Relates to the {@code JMSXDeliveryCount} message property.
   *
   * {@code true} if the message may have been delivered in the past
   */
  private Boolean redelivered;

  /**
   * Present only in JMS 2.0 Messages
   * <p>
   * JMS provider calculates its delivery time by adding the {@code deliveryDelay}
   * value specified on the send method to the time the message was sent (for transacted sends,
   * this is the time the client sends the message, not the time the transaction is committed).
   * <p>
   * A message's delivery time is the earliest time when a provider may make the message visible
   * on the target destination and available for delivery to consumers.
   *
   * @return the message's delivery time or {@code zero} if no {@code deliveryDelay} was set
   */
  private Long deliveryTime;

  /**
   * {@inheritDoc}
   */
  @Override
  public String getJMSMessageID() {
    return messageId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getJMSTimestamp() {
    return timestamp;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getJMSCorrelationID() {
    return correlactionId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JmsDestination getJMSReplyTo() {
    return replyTo;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JmsDestination getJMSDestination() {
    return destination;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Integer getJMSDeliveryMode() {
    return deliveryMode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Boolean getJMSRedelivered() {
    return redelivered;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getJMSType() {
    return type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getJMSExpiration() {
    return expiration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getJMSDeliveryTime() {
    return deliveryTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Integer getJMSPriority() {
    return priority;
  }

  public static class Builder {

    private DefaultJmsHeaders jmsHeaders = new DefaultJmsHeaders();

    public Builder setMessageId(String messageId) {
      jmsHeaders.messageId = messageId;
      return this;
    }

    public Builder setTimestamp(long timestamp) {
      jmsHeaders.timestamp = timestamp;
      return this;
    }

    public Builder setCorrelactionId(String correlationId) {
      jmsHeaders.correlactionId = correlationId;
      return this;
    }

    public Builder setReplyTo(JmsDestination replyTo) {
      jmsHeaders.replyTo = replyTo;
      return this;
    }

    public Builder setDestination(JmsDestination destination) {
      jmsHeaders.destination = destination;
      return this;
    }

    public Builder setDeliveryMode(int deliveryMode) {
      jmsHeaders.deliveryMode = deliveryMode;
      return this;
    }

    public Builder setRedelivered(boolean redelivered) {
      jmsHeaders.redelivered = redelivered;
      return this;
    }

    public Builder setType(String type) {
      jmsHeaders.type = type;
      return this;
    }

    public Builder setExpiration(long expiration) {
      jmsHeaders.expiration = expiration;
      return this;
    }

    public Builder setPriority(int priority) {
      jmsHeaders.priority = priority;
      return this;
    }

    public Builder setDeliveryTime(long deliveryTime) {
      jmsHeaders.deliveryTime = deliveryTime;
      return this;
    }

    public DefaultJmsHeaders build() {
      return jmsHeaders;
    }
  }
}
