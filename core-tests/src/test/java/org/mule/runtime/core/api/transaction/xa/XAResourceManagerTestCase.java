/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transaction.xa;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

import org.mule.runtime.core.internal.transaction.xa.AbstractTransactionContext;
import org.mule.runtime.core.internal.transaction.xa.AbstractXAResourceManager;
import org.mule.runtime.core.internal.transaction.xa.AbstractXaTransactionContext;
import org.junit.Test;

import javax.transaction.xa.Xid;


public class XAResourceManagerTestCase {

  @Test(expected = ResourceManagerException.class)
  public void prepareTxNotReady() throws ResourceManagerException {
    TestXaResourceManager manager = new TestXaResourceManager();
    manager.prepareTransaction(mock(AbstractXaTransactionContext.class));
    assertThat(manager.isPrepared(), is(true));
  }

  @Test
  public void prepareTx() throws ResourceManagerException {
    TestXaResourceManager manager = new TestXaResourceManager();
    manager.start();
    manager.prepareTransaction(mock(AbstractXaTransactionContext.class));
    assertThat(manager.isPrepared(), is(true));
  }

  @Test
  public void activeTransactionResource() {
    Xid xid = mock(Xid.class);
    AbstractXaTransactionContext context = mock(AbstractXaTransactionContext.class);
    TestXaResourceManager manager = new TestXaResourceManager();
    manager.addActiveTransactionalResource(xid, context);
    assertThat(manager.getTransactionalResource(xid), is(context));
  }

  @Test
  public void suspendedTransactionResource() {
    Xid xid = mock(Xid.class);
    AbstractXaTransactionContext context = mock(AbstractXaTransactionContext.class);
    TestXaResourceManager manager = new TestXaResourceManager();
    manager.addSuspendedTransactionalResource(xid, context);
    assertThat(manager.getTransactionalResource(xid), is(context));
  }

  @Test
  public void removeTransactionResource() {
    Xid xid = mock(Xid.class);
    AbstractXaTransactionContext context = mock(AbstractXaTransactionContext.class);
    AbstractXaTransactionContext context2 = mock(AbstractXaTransactionContext.class);
    TestXaResourceManager manager = new TestXaResourceManager();
    manager.addActiveTransactionalResource(xid, context);
    manager.removeActiveTransactionalResource(xid);
    manager.addSuspendedTransactionalResource(xid, context2);
    assertThat(manager.getTransactionalResource(xid), is(context2));
  }

  @Test
  public void removeSuspendedTransactionResource() {
    Xid xid = mock(Xid.class);
    AbstractXaTransactionContext context = mock(AbstractXaTransactionContext.class);
    AbstractXaTransactionContext context2 = mock(AbstractXaTransactionContext.class);
    TestXaResourceManager manager = new TestXaResourceManager();
    manager.addActiveTransactionalResource(xid, context);
    manager.removeActiveTransactionalResource(xid);
    manager.addSuspendedTransactionalResource(xid, context2);
    manager.removeSuspendedTransactionalResource(xid);
    assertThat(manager.getTransactionalResource(xid), is(nullValue()));
  }

  private static final class TestXaResourceManager extends AbstractXAResourceManager<AbstractXaTransactionContext> {

    private boolean isPrepared = false;

    public boolean isPrepared() {
      return isPrepared;
    }

    @Override
    protected int doPrepare(AbstractXaTransactionContext context) throws ResourceManagerException {
      isPrepared = true;
      return 0;
    }

    @Override
    protected void doBegin(AbstractTransactionContext context) {

    }

    @Override
    protected void doCommit(AbstractTransactionContext context) throws ResourceManagerException {

    }

    @Override
    protected void doRollback(AbstractTransactionContext context) throws ResourceManagerException {

    }
  }
}
