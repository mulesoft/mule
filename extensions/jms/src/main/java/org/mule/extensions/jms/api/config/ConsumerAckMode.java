/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.config;

import org.mule.extensions.jms.api.operation.JmsConsume;
import org.mule.extensions.jms.api.source.JmsListener;
import org.mule.extensions.jms.internal.config.InternalAckMode;

import javax.jms.Session;

/**
 * Declares the kind of Acknowledgement mode supported for consumer operations.
 *
 * AUTO: Mule ACKs the message only if the flow is finished successfully.
 * MANUAL: This is JMS {@link Session#CLIENT_ACKNOWLEDGE} mode. The user must do the ack manually within the flow.
 * DUPS_OK: JMS message is acked automatically but in a lazy fashion which may lead to duplicates.
 * NONE: Mule automatically ACKs the message upon reception.
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

  ConsumerAckMode(InternalAckMode none) {
    this.ackMode = none;
  }

  @Override
  public InternalAckMode getInternalAckMode() {
    return ackMode;
  }
}
