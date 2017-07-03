/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.journal.queue;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.core.internal.util.journal.JournalEntry;
import org.mule.runtime.core.internal.util.journal.JournalEntrySerializer;
import org.mule.runtime.core.internal.util.journal.TransactionCompletePredicate;
import org.mule.runtime.core.internal.util.journal.TransactionJournal;
import org.mule.runtime.core.internal.util.queue.QueueStore;

import com.google.common.collect.Multimap;

import java.io.Serializable;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation for a queue transaction journal.
 *
 * @param <T> type of transaction identifier
 * @param <K> type of the actual journal entry which must extend {@link JournalEntry}
 */
public abstract class AbstractQueueTransactionJournal<T, K extends JournalEntry<T>> {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  private TransactionJournal<T, K> logFile;

  public AbstractQueueTransactionJournal(String logFilesDirectory, JournalEntrySerializer journalEntrySerializer,
                                         Integer maximumFileSizeInMegabytes) {
    checkArgument(maximumFileSizeInMegabytes == null || maximumFileSizeInMegabytes > 0,
                  "Maximum tx log file size needs to be greater than zero");
    this.logFile = new TransactionJournal(logFilesDirectory, new TransactionCompletePredicate() {

      @Override
      public boolean isTransactionComplete(JournalEntry journalEntry) {
        AbstractQueueTxJournalEntry abstractQueueTxJournalEntry = (AbstractQueueTxJournalEntry) journalEntry;
        return abstractQueueTxJournalEntry.isCommit() || abstractQueueTxJournalEntry.isRollback();
      }
    }, journalEntrySerializer, maximumFileSizeInMegabytes);
  }

  public void logAdd(T txId, QueueStore queue, Serializable value) {
    if (logger.isDebugEnabled()) {
      logger.debug("Logging queue add operation for tx " + txId);
    }
    logFile.logUpdateOperation(createUpdateJournalEntry(txId, AbstractQueueTxJournalEntry.Operation.ADD.getByteRepresentation(),
                                                        queue.getName(), value));
  }

  public void logAddFirst(T txId, QueueStore queue, Serializable item) {
    if (logger.isDebugEnabled()) {
      logger.debug("Logging queue add first operation for tx " + txId);
    }
    logFile.logUpdateOperation(createUpdateJournalEntry(txId,
                                                        AbstractQueueTxJournalEntry.Operation.ADD_FIRST.getByteRepresentation(),
                                                        queue.getName(), item));
  }

  public void logRemove(T txId, QueueStore queue, Serializable value) {
    if (logger.isDebugEnabled()) {
      logger.debug("Logging queue remove operation for tx " + txId);
    }
    logFile
        .logUpdateOperation(createUpdateJournalEntry(txId, AbstractQueueTxJournalEntry.Operation.REMOVE.getByteRepresentation(),
                                                     queue.getName(), value));
  }

  public void logCommit(T txId) {
    if (logger.isDebugEnabled()) {
      logger.debug("Logging queue commit operation for tx " + txId);
    }
    logFile.logCheckpointOperation(createCheckpointJournalEntry(txId, AbstractQueueTxJournalEntry.Operation.COMMIT
        .getByteRepresentation()));
  }

  /**
   * Creates a {@link JournalEntry} for an update operation in the queue.
   *
   * @param txId transaction identifier
   * @param operation operation done over the queue
   * @param queueName queueName of the queue in which the operation has been done
   * @param serialize value of the operation
   * @return a new {@link JournalEntry}
   */
  protected abstract K createUpdateJournalEntry(T txId, byte operation, String queueName, Serializable serialize);

  /**
   * Creates a checkpoint {@link JournalEntry}.
   *
   * @param txId transaction identifier
   * @param operation checkpoint operation
   * @return a new {@link JournalEntry}
   */
  protected abstract K createCheckpointJournalEntry(T txId, byte operation);

  public void logRollback(T txId) {
    if (logger.isDebugEnabled()) {
      logger.debug("Logging queue rollback operation for tx " + txId);
    }
    logFile.logCheckpointOperation(createCheckpointJournalEntry(txId, AbstractQueueTxJournalEntry.Operation.ROLLBACK
        .getByteRepresentation()));
  }

  public synchronized void close() {
    logFile.close();
  }

  public synchronized void clear() {
    logFile.clear();
  }

  public Multimap<T, K> getAllLogEntries() {
    return logFile.getAllLogEntries();
  }

  public Collection<K> getLogEntriesForTx(T txId) {
    return logFile.getLogEntriesForTx(txId);
  }

  protected TransactionJournal<T, K> getJournal() {
    return logFile;
  }

}
