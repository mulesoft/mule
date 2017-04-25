/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.connection.session;

import org.mule.extensions.jms.internal.connection.JmsSession;

/**
 * Object to save the information about the current transaction.
 *
 * @since 4.0
 */
final class TransactionInformation {

  private JmsSession jmsSession;
  private TransactionStatus transactionStatus;

  TransactionInformation() {}

  JmsSession getJmsSession() {
    return jmsSession;
  }

  void setJmsSession(JmsSession jmsSession) {
    this.jmsSession = jmsSession;
  }

  TransactionStatus getTransactionStatus() {
    return transactionStatus;
  }

  void setTransactionStatus(TransactionStatus transactionStatus) {
    this.transactionStatus = transactionStatus;
  }
}
