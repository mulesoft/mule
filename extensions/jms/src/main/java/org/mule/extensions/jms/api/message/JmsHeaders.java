/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.message;

import org.mule.extensions.jms.api.destination.JmsDestination;

import javax.jms.DeliveryMode;

/**
 * JMS header fields contain values used by both clients and providers to identify and route messages.
 * A message's complete header is transmitted to all JMS clients that receive the message.
 *
 * @since 4.0
 */
public interface JmsHeaders {

  /**
   * @return the destination to which the message is being sent
   */
  JmsDestination getJMSDestination();

  /**
   * @return the delivery mode specified when the message was sent, which
   * can be either {@link DeliveryMode#PERSISTENT} or {@link DeliveryMode#NON_PERSISTENT}
   */
  Integer getJMSDeliveryMode();

  /**
   * JMS provider calculates its expiration time by adding the {@code timeToLive}
   * value specified on the send method to the time the message was sent (for transacted sends,
   * this is the time the client sends the message, not the time the transaction is committed)
   * <p>
   * If the {@code timeToLive} is specified as {@code zero}, the message's expiration time is
   * set to zero to indicate that the message does not expire.
   *
   * @return the message's expiration time or {@code zero} if the message does not expire
   */
  Long getJMSExpiration();

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
  Long getJMSDeliveryTime();

  /**
   * JMS defines a ten level priority value with 0 as the lowest priority and 9 as the highest.
   * In addition, clients should consider priorities 0-4 as gradations of {@code normal} priority
   * and priorities 5-9 as gradations of {@code expedited} priority.
   * <p>
   * JMS does not require that a provider strictly implement priority ordering of messages;
   * however, it should do its best to deliver expedited messages ahead of normal messages.
   *
   * @return the message priority level.
   */
  Integer getJMSPriority();

  /**
   * @return a value that uniquely identifies each message sent by a provider.
   * If the Producer was hinted to {@code disableMessageID}, then {@code null}
   * is returned.
   */
  String getJMSMessageID();

  /**
   * @return he time a message was handed off to a provider to be sent.
   * It is not the time the message was actually transmitted because the actual send
   * may occur later due to transactions or other client side queueing of messages.
   * <p>
   * If the Producer was hinted to {@code disableMessageTimestamp}, then {@code zero}
   * is returned.
   */
  Long getJMSTimestamp();

  /**
   * Used to link one message with another. A typical use is to link a response
   * message with its request message, using its messageID
   *
   * @return the message correlationId
   */
  String getJMSCorrelationID();

  /**
   * @return the name of the Destination supplied by a client when a message is sent,
   * where a reply to the message should be sent.
   * If no {@code replyTo} destination was set, then {@code null} is returned.
   */
  JmsDestination getJMSReplyTo();

  /**
   * @return a message type identifier supplied by a client when a message is sent.
   */
  String getJMSType();

  /**
   * If {@code true}, it is likely, but not guaranteed,
   * that this message was delivered but not acknowledged in the past.
   * Relates to the {@code JMSXDeliveryCount} message property.
   *
   * @return {@code true} if the message may have been delivered in the past
   */
  Boolean getJMSRedelivered();

}
