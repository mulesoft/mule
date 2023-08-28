/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.journal.queue;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.util.journal.JournalEntry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * {@link JournalEntry} for a local queue transaction
 */
public class LocalQueueTxJournalEntry extends AbstractQueueTxJournalEntry<Integer> {

  public LocalQueueTxJournalEntry(int txId, byte operation, String queueName, Serializable value) {
    super(txId, operation, queueName, value);
  }

  protected LocalQueueTxJournalEntry(int txId, byte operation) {
    super(txId, operation);
  }

  public LocalQueueTxJournalEntry(DataInputStream inputStream, MuleContext muleContext) throws IOException {
    super(inputStream, muleContext);
  }

  @Override
  protected Integer deserializeTxId(DataInputStream inputStream) throws IOException {
    return inputStream.readInt();
  }

  @Override
  protected void serializeTxId(DataOutputStream outputStream) throws IOException {
    outputStream.writeInt(getTxId());
  }

}

