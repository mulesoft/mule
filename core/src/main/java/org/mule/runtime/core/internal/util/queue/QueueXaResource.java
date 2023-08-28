/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.queue;

import static java.util.Objects.requireNonNull;

import org.mule.runtime.core.internal.transaction.xa.AbstractXAResourceManager;
import org.mule.runtime.core.internal.transaction.xa.DefaultXASession;
import org.mule.runtime.core.internal.util.xa.XaTransactionRecoverer;

import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

public class QueueXaResource extends DefaultXASession<XaQueueTypeTransactionContextAdapter> {

  private final XaTransactionRecoverer xaTransactionRecoverer;
  private final QueueProvider queueProvider;

  public QueueXaResource(AbstractXAResourceManager<XaQueueTypeTransactionContextAdapter> xaResourceManager,
                         XaTransactionRecoverer xaTransactionRecoverer,
                         QueueProvider queueProvider) {
    super(xaResourceManager);
    this.xaTransactionRecoverer = requireNonNull(xaTransactionRecoverer);
    this.queueProvider = requireNonNull(queueProvider);
  }

  // XA transaction implementation
  @Override
  protected void commitDanglingTransaction(Xid xid, boolean onePhase) throws XAException {
    xaTransactionRecoverer.commitDandlingTransaction(xid, onePhase);
  }

  @Override
  protected void rollbackDandlingTransaction(Xid xid) throws XAException {
    xaTransactionRecoverer.rollbackDandlingTransaction(xid);
  }

  @Override
  protected XaQueueTypeTransactionContextAdapter createTransactionContext(Xid xid) {
    return new XaQueueTypeTransactionContextAdapter(xaTransactionRecoverer.getXaTxQueueTransactionJournal(), queueProvider, xid);
  }

  @Override
  public Xid[] recover(int i) throws XAException {
    return xaTransactionRecoverer.recover(i);
  }
}
