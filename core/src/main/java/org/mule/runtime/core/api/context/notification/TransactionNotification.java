/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import static java.util.Optional.empty;

import org.mule.runtime.core.api.transaction.Transaction;

// TODO: Shouldn't really be an EnrichedServerNotification as it doesn't know event data,
// but inherits to be compatible with how mule-agent tracking currently handles them
public class TransactionNotification extends EnrichedServerNotification implements SynchronousServerEvent {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -3245036187011582121L;

  private static String UNKNOWN_APPLICATION_NAME = "unknown";

  public static final int TRANSACTION_BEGAN = TRANSACTION_EVENT_ACTION_START_RANGE + 1;
  public static final int TRANSACTION_COMMITTED = TRANSACTION_EVENT_ACTION_START_RANGE + 2;
  public static final int TRANSACTION_ROLLEDBACK = TRANSACTION_EVENT_ACTION_START_RANGE + 3;

  static {
    registerAction("begin", TRANSACTION_BEGAN);
    registerAction("commit", TRANSACTION_COMMITTED);
    registerAction("rollback", TRANSACTION_ROLLEDBACK);
  }

  private String applicationName;

  /**
   * Ideally, that should've been a transaction's XID, but we'd need to resort to all kinds of reflection tricks to get it. Still,
   * toString() typically outputs a class name followed by the XID, so that's good enough.
   */
  private String transactionStringId;

  public TransactionNotification(Transaction transaction, int action) {
    this(transaction, action, UNKNOWN_APPLICATION_NAME);
  }

  public TransactionNotification(Transaction transaction, int action, String applicationName) {
    super(emptyInfo(), action, transaction.getId());
    this.transactionStringId = transaction.getId();
    this.applicationName = applicationName;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public String getTransactionStringId() {
    return this.transactionStringId;
  }

  @Override
  public String toString() {
    return EVENT_NAME + "{" + "action=" + getActionName(action) + ", transactionStringId=" + transactionStringId + ", timestamp="
        + timestamp + "}";
  }

  private static EnrichedNotificationInfo emptyInfo() {
    return new EnrichedNotificationInfo(null, null, null, null);
  }
}
