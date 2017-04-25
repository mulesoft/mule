/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.config;

import org.mule.extensions.jms.internal.config.JmsAckMode;
import org.mule.extensions.jms.internal.operation.JmsConsume;
import org.mule.extensions.jms.internal.source.JmsListener;
import org.mule.extensions.jms.internal.config.InternalAckMode;

import javax.jms.Session;

/**
 * Declares the kind of Acknowledgement mode supported for consumer operations.
 *
 * <ul>
 *     <li><b>MANUAL</b>: This is JMS {@link Session#CLIENT_ACKNOWLEDGE} mode. The user must do the ack manually within the flow. </li>
 *     <li><b>DUPS_OK</b>: JMS message is acked automatically but in a lazy fashion which may lead to duplicates. </li>
 *     <li><b>NONE</b>: Mule automatically ACKs the message upon reception. </li>
 * </ul>
 *
 * @since 4.0
 */
public enum ConsumerAckMode implements JmsAckMode {
  /**
   * This is JMS {@link Session#AUTO_ACKNOWLEDGE} mode.
   * The session automatically acknowledges the receipt when it successfully delivered the message
   * to a {@link JmsConsume#consume} or {@link JmsListener} handler.
   */
  AUTO(InternalAckMode.AUTO),

  /**
   * This is JMS {@link Session#CLIENT_ACKNOWLEDGE} mode. The user must do the ACK manually within the flow
   */
  MANUAL(InternalAckMode.MANUAL),

  /**
   * Similar to AUTO, the JMS message is acknowledged automatically but in a lazy fashion which may lead to duplicates.
   */
  DUPS_OK(InternalAckMode.DUPS_OK);

  private InternalAckMode ackMode;

  ConsumerAckMode(InternalAckMode ackMode) {
    this.ackMode = ackMode;
  }

  @Override
  public InternalAckMode getInternalAckMode() {
    return ackMode;
  }
}
