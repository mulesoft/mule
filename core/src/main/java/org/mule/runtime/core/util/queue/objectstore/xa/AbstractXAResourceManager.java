/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.queue.objectstore.xa;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.xa.Xid;

/**
 * @deprecated this class will be removed in Mule 4.0 in favor of the new queue implementation
 */
@Deprecated
public abstract class AbstractXAResourceManager extends AbstractResourceManager {

  protected Map<Xid, AbstractTransactionContext> suspendedContexts = new ConcurrentHashMap<Xid, AbstractTransactionContext>();
  protected Map<Xid, AbstractTransactionContext> activeContexts = new ConcurrentHashMap<Xid, AbstractTransactionContext>();

  public AbstractXAResourceManager() {
    super();
  }

  protected boolean includeBranchInXid() {
    return true;
  }

  protected AbstractTransactionContext getTransactionalResource(Xid xid) {
    AbstractTransactionContext context = getActiveTransactionalResource(xid);
    if (context != null) {
      return context;
    } else {
      return getSuspendedTransactionalResource(xid);
    }
  }

  AbstractTransactionContext getActiveTransactionalResource(Xid xid) {
    return activeContexts.get(xid);
  }

  AbstractTransactionContext getSuspendedTransactionalResource(Xid xid) {
    return suspendedContexts.get(xid);
  }

  void addActiveTransactionalResource(Xid xid, AbstractTransactionContext context) {
    activeContexts.put(xid, context);
  }

  void addSuspendedTransactionalResource(Xid xid, AbstractTransactionContext context) {
    suspendedContexts.put(xid, context);
  }

  void removeActiveTransactionalResource(Xid xid) {
    activeContexts.remove(xid);
  }

  void removeSuspendedTransactionalResource(Xid xid) {
    suspendedContexts.remove(xid);
  }
}
