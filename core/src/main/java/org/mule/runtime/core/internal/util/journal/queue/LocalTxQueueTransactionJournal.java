/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.journal.queue;

import org.mule.runtime.api.serialization.SerializationProtocol;
import org.mule.runtime.core.internal.util.journal.JournalEntrySerializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Journal for operations done to a queue within a transactional context
 */
public class LocalTxQueueTransactionJournal extends AbstractQueueTransactionJournal<Integer, LocalQueueTxJournalEntry> {

  public LocalTxQueueTransactionJournal(String logFilesDirectory, final SerializationProtocol serializer,
                                        int maximumFileSizeInMegabytes) {
    super(logFilesDirectory, createLocalTxQueueJournalEntrySerializer(serializer), maximumFileSizeInMegabytes);
  }

  public LocalTxQueueTransactionJournal(String logFilesDirectory, final SerializationProtocol serializer) {
    super(logFilesDirectory, createLocalTxQueueJournalEntrySerializer(serializer), null);
  }

  public static JournalEntrySerializer<Integer, LocalQueueTxJournalEntry> createLocalTxQueueJournalEntrySerializer(final SerializationProtocol serializer) {
    return new JournalEntrySerializer<>() {

      @Override
      public LocalQueueTxJournalEntry deserialize(DataInputStream inputStream) throws IOException {
        return new LocalQueueTxJournalEntry(inputStream, serializer);
      }

      @Override
      public void serialize(LocalQueueTxJournalEntry journalEntry, DataOutputStream dataOutputStream) {
        journalEntry.write(dataOutputStream, serializer);
      }
    };
  }

  @Override
  protected LocalQueueTxJournalEntry createUpdateJournalEntry(Integer txId, byte byteRepresentation, String queueName,
                                                              Serializable value) {
    return new LocalQueueTxJournalEntry(txId, byteRepresentation, queueName, value);
  }

  @Override
  protected LocalQueueTxJournalEntry createCheckpointJournalEntry(Integer txId, byte operation) {
    return new LocalQueueTxJournalEntry(txId, operation);
  }

}
