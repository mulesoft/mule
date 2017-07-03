/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.journal.queue;

import static org.apache.commons.collections.CollectionUtils.find;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.internal.util.queue.QueueProvider;
import org.mule.runtime.core.internal.util.queue.RecoverableQueueStore;

import com.google.common.collect.Multimap;

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.collections.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process for recover pending transactions after a server crash.
 *
 * This process must be executed before accessing the transactional queues.
 */
public class LocalTxQueueTransactionRecoverer {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private final LocalTxQueueTransactionJournal localTxQueueTransactionJournal;
  private final QueueProvider queueProvider;

  public LocalTxQueueTransactionRecoverer(LocalTxQueueTransactionJournal localTxQueueTransactionJournal,
                                          QueueProvider queueProvider) {
    this.localTxQueueTransactionJournal = localTxQueueTransactionJournal;
    this.queueProvider = queueProvider;
  }

  /**
   * Recover all the pending transactions.
   *
   * Will undo all operations done over queues that were not commit or rolled back.
   *
   * Clears the transaction log after processing all the log entries since does entries are not longer required.
   */
  public void recover() {
    if (logger.isDebugEnabled()) {
      logger.debug("Executing transaction recovery");
    }
    Multimap<Integer, LocalQueueTxJournalEntry> allEntries = this.localTxQueueTransactionJournal.getAllLogEntries();
    if (logger.isDebugEnabled()) {
      logger.debug("Found " + allEntries.size() + " txs to recover");
    }
    int txRecovered = 0;
    for (Integer txId : allEntries.keySet()) {
      Collection<LocalQueueTxJournalEntry> entries = allEntries.get(txId);
      Object commitOrRollback = find(entries, new Predicate() {

        @Override
        public boolean evaluate(Object object) {
          LocalQueueTxJournalEntry logEntry = (LocalQueueTxJournalEntry) object;
          return logEntry.isCommit() || logEntry.isRollback();
        }
      });
      if (commitOrRollback != null) {
        continue;
      }
      txRecovered++;
      for (LocalQueueTxJournalEntry logEntry : entries) {
        if (logEntry.isRemove()) {
          String queueName = logEntry.getQueueName();
          RecoverableQueueStore queue = queueProvider.getRecoveryQueue(queueName);
          Serializable polledValue = logEntry.getValue();
          if (!queue.contains(polledValue)) {
            if (logger.isDebugEnabled()) {
              logger.debug("re-adding polled element that was not commited to queue " + queue.getName());
            }
            try {
              queue.putNow(polledValue);
            } catch (InterruptedException e) {
              throw new MuleRuntimeException(e);
            }
          }
        } else if (logEntry.isAdd() || logEntry.isAddFirst()) {
          Serializable offeredValue = logEntry.getValue();
          String queueName = logEntry.getQueueName();
          RecoverableQueueStore queue = queueProvider.getRecoveryQueue(queueName);
          if (queue.contains(offeredValue)) {
            if (logger.isDebugEnabled()) {
              logger.debug("removing offer element that was not commited to queue " + queue.getName());
            }
            queue.remove(offeredValue);
          }
        }
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Recovered " + txRecovered + " txs to recover");
    }
    this.localTxQueueTransactionJournal.clear();
  }

}
