/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.util.journal.queue.LocalTxQueueTransactionJournal;

public class TestTransactionLogger extends LocalTxQueueTransactionJournal {

  private boolean failDuringLogCommit;

  public TestTransactionLogger(String logFilesDirectory, MuleContext muleContext) {
    super(logFilesDirectory, muleContext);
  }

  public TestTransactionLogger failDuringLogCommit() {
    this.failDuringLogCommit = true;
    return this;
  }

  @Override
  public void logCommit(Integer txId) {
    if (failDuringLogCommit) {
      throw new RuntimeException("faked failure");
    }
    super.logCommit(txId);
  }
}
