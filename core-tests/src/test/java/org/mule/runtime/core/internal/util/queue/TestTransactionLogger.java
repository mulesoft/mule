/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
