/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.flow;

import org.mule.runtime.core.api.transaction.TransactionConfig;

/**
 * Helper to access constants from {@link TransactionConfig}.
 */
public enum TransactionConfigEnum {
  /**
   * Whether there is a transaction available or not, ignore it
   * <p>
   * J2EE: NotSupported
   */
  ACTION_NONE((byte) 0),

  /**
   * Will ensure that a new transaction is created for each invocation
   * <p>
   * J2EE RequiresNew
   */
  ACTION_ALWAYS_BEGIN((byte) 1),

  /**
   * Will begin a new transaction if no transaction is already present
   * <p>
   * J2EE: Required
   */
  ACTION_BEGIN_OR_JOIN((byte) 2),

  /**
   * There must always be a transaction present for the invocation
   * <p>
   * J2EE: Mandatory
   */
  ACTION_ALWAYS_JOIN((byte) 3),

  /**
   * If there is a transaction available, then use it, otherwise continue processing
   * <p>
   * J2EE: Supports
   */
  ACTION_JOIN_IF_POSSIBLE((byte) 4),

  /**
   * There must not be a transaction present for the invocation
   * <p>
   * J2EE Never
   */
  ACTION_NEVER((byte) 5),

  /**
   * Be indifferent to any active transaction. Don;t check for one, and if there is one, don;t commit or abort it
   * <p>
   */
  ACTION_INDIFFERENT((byte) 6),

  /*
   * Executes outside any existent transaction
   */
  ACTION_NOT_SUPPORTED((byte) 7),

  /**
   * Transaction action by default. Note that before 3.2 it was ACTION_NONE
   * <p>
   */
  ACTION_DEFAULT(ACTION_INDIFFERENT.getAction());

  private byte action;

  private TransactionConfigEnum(byte action) {
    this.action = action;
  }

  /**
   * @return the constant value that is defined in {@link TransactionConfig}.
   */
  public byte getAction() {
    return action;
  }
}
