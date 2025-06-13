/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.xa;

import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;
import org.mule.runtime.core.internal.util.journal.queue.XaQueueTxJournalEntry;
import org.mule.runtime.core.internal.util.journal.queue.XaTxQueueTransactionJournal;
import org.mule.runtime.core.internal.util.queue.PersistentXaTransactionContext;
import org.mule.runtime.core.internal.util.queue.QueueProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.google.common.collect.Multimap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XaTransactionRecoverer {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  private final XaTxQueueTransactionJournal xaTxQueueTransactionJournal;
  private final QueueProvider queueProvider;

  public XaTransactionRecoverer(XaTxQueueTransactionJournal xaTxQueueTransactionJournal, QueueProvider queueProvider) {
    this.xaTxQueueTransactionJournal = xaTxQueueTransactionJournal;
    this.queueProvider = queueProvider;
  }

  public XaTxQueueTransactionJournal getXaTxQueueTransactionJournal() {
    return xaTxQueueTransactionJournal;
  }

  public synchronized Xid[] recover(int flag) throws XAException {
    // No need to do anything for XAResource.TMENDRSCAN
    if (flag == XAResource.TMENDRSCAN) {
      return new Xid[0];
    }
    // For XAResource.TMSTARTRSCAN and XAResource.TMNOFLAGS (only possible values despite XAResource.TMENDRSCAN we returns
    // the set of Xid to recover (no commit, no rollback) and bitronix will commit, rollback for Xid that are
    // dangling transactions and will do nothing for those that are currently being executed.
    Multimap<Xid, XaQueueTxJournalEntry> xidXaJournalEntryMultimap = xaTxQueueTransactionJournal.getAllLogEntries();
    logger.debug("Executing XA recover; Found {} in the tx log", xidXaJournalEntryMultimap.size());
    List<Xid> txsToRecover = new ArrayList<>();
    for (Xid xid : xidXaJournalEntryMultimap.keySet()) {
      Collection<XaQueueTxJournalEntry> entries = xidXaJournalEntryMultimap.get(xid);

      if (entries.stream().anyMatch(logEntry -> logEntry.isCommit() || logEntry.isRollback())) {
        continue;
      }
      txsToRecover.add(xid);
    }
    logger.debug("found {} txs to recover", txsToRecover.size());
    return txsToRecover.toArray(new Xid[txsToRecover.size()]);
  }

  public void rollbackDandlingTransaction(Xid xid) throws XAException {
    try {
      logger.info("Rollbacking dangling tx with id {}", xid);
      new PersistentXaTransactionContext(xaTxQueueTransactionJournal, queueProvider, xid).doRollback();
    } catch (ResourceManagerException e) {
      logger.warn(e.getMessage());
      logger.debug("Error rollbacking dangling transaction", e);
      throw new XAException(XAException.XAER_NOTA);
    }
  }

  public void commitDandlingTransaction(Xid xid, boolean onePhase) throws XAException {
    try {
      logger.info("Commiting dangling tx with id {}", xid);
      new PersistentXaTransactionContext(xaTxQueueTransactionJournal, queueProvider, xid).doCommit();
    } catch (ResourceManagerException e) {
      logger.warn(e.getMessage());
      logger.debug("Error committing dangling transaction", e);
      throw new XAException(XAException.XAER_NOTA);
    }
  }
}
