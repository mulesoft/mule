/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.xa;

import static org.apache.commons.collections.CollectionUtils.find;
import org.mule.runtime.core.internal.util.journal.queue.XaQueueTxJournalEntry;
import org.mule.runtime.core.internal.util.journal.queue.XaTxQueueTransactionJournal;
import org.mule.runtime.core.internal.util.queue.PersistentXaTransactionContext;
import org.mule.runtime.core.internal.util.queue.QueueProvider;
import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;

import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.collections.Predicate;
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
    if (logger.isDebugEnabled()) {
      logger.debug("Executing XA recover");
      logger.debug("Found " + xidXaJournalEntryMultimap.size() + " in the tx log");
    }
    List<Xid> txsToRecover = new ArrayList<Xid>();
    for (Xid xid : xidXaJournalEntryMultimap.keySet()) {
      Collection<XaQueueTxJournalEntry> entries = xidXaJournalEntryMultimap.get(xid);
      Object commitOrRollback = find(entries, new Predicate() {

        @Override
        public boolean evaluate(Object object) {
          XaQueueTxJournalEntry logEntry = (XaQueueTxJournalEntry) object;
          return logEntry.isCommit() || logEntry.isRollback();
        }
      });
      if (commitOrRollback != null) {
        continue;
      }
      txsToRecover.add(xid);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("found " + txsToRecover.size() + " txs to recover");
    }
    return txsToRecover.toArray(new Xid[txsToRecover.size()]);
  }

  public void rollbackDandlingTransaction(Xid xid) throws XAException {
    try {
      logger.info("Rollbacking dangling tx with id " + xid);
      new PersistentXaTransactionContext(xaTxQueueTransactionJournal, queueProvider, xid).doRollback();
    } catch (ResourceManagerException e) {
      logger.warn(e.getMessage());
      if (logger.isDebugEnabled()) {
        logger.debug("Error rollbacking dangling transaction", e);
      }
      throw new XAException(XAException.XAER_NOTA);
    }
  }

  public void commitDandlingTransaction(Xid xid, boolean onePhase) throws XAException {
    try {
      logger.info("Commiting dangling tx with id " + xid);
      new PersistentXaTransactionContext(xaTxQueueTransactionJournal, queueProvider, xid).doCommit();
    } catch (ResourceManagerException e) {
      logger.warn(e.getMessage());
      if (logger.isDebugEnabled()) {
        logger.debug("Error committing dangling transaction", e);
      }
      throw new XAException(XAException.XAER_NOTA);
    }
  }
}
