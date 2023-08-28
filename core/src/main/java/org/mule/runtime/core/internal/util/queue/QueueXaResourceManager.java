/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;
import org.mule.runtime.core.internal.transaction.xa.AbstractTransactionContext;
import org.mule.runtime.core.internal.transaction.xa.AbstractXAResourceManager;

import javax.transaction.xa.XAResource;

public class QueueXaResourceManager extends AbstractXAResourceManager<XaQueueTypeTransactionContextAdapter> {

  @Override
  protected void doBegin(AbstractTransactionContext context) {
    // Nothing special to do
  }

  @Override
  protected int doPrepare(XaQueueTypeTransactionContextAdapter context) throws ResourceManagerException {
    context.doPrepare();
    return XAResource.XA_OK;
  }

  @Override
  protected void doCommit(AbstractTransactionContext context) throws ResourceManagerException {
    context.doCommit();
  }

  @Override
  protected void doRollback(AbstractTransactionContext context) throws ResourceManagerException {
    context.doRollback();
  }

}
