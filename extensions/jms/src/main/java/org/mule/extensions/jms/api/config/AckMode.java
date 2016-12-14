/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.config;

import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static javax.jms.Session.CLIENT_ACKNOWLEDGE;
import static javax.jms.Session.DUPS_OK_ACKNOWLEDGE;
import static javax.jms.Session.SESSION_TRANSACTED;
import org.mule.extensions.jms.api.operation.JmsConsume;
import org.mule.extensions.jms.api.source.JmsSubscribe;

import javax.jms.Session;

/**
 * Declares the kind of Acknowledgement mode supported.
 * If a session is transacted, message acknowledgment is handled automatically by {@code commit},
 * and recovery is handled automatically by {@code rollback}.
 *
 * If a session is not transacted, there are four acknowledgment options:
 *
 * AUTO: Mule ACKs the message only if the flow is finished successfully.
 * MANUAL: This is JMS {@link Session#CLIENT_ACKNOWLEDGE} mode. The user must do the ack manually within the flow.
 * DUPS_OK: JMS message is acked automatically but in a lazy fashion which may lead to duplicates.
 * NONE: Mule automatically ACKs the message upon reception.
 *
 * @since 4.0
 */
public enum AckMode {

  /**
   * Mule automatically ACKs the message upon reception
   */
  NONE(0),

  /**
   * This is JMS {@link Session#AUTO_ACKNOWLEDGE} mode.
   * The session automatically acknowledges the receipt when it successfully delivered the message
   * to a {@link JmsConsume#consume} or {@link JmsSubscribe} handler.
   */
  AUTO(AUTO_ACKNOWLEDGE),

  /**
   * This is JMS {@link Session#CLIENT_ACKNOWLEDGE} mode. The user must do the ACK manually within the flow
   */
  MANUAL(CLIENT_ACKNOWLEDGE),

  /**
   * Similar to AUTO, the JMS message is acknowledged automatically but in a lazy fashion which may lead to duplicates.
   */
  DUPS_OK(DUPS_OK_ACKNOWLEDGE),

  /**
   * Transacted Session don't have ACK
   */
  TRANSACTED(SESSION_TRANSACTED);

  private final int ackMode;

  AckMode(int ackMode) {
    this.ackMode = ackMode;
  }

  public int getAckMode() {
    return ackMode;
  }

}
