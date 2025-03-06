/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transaction.xa;

import static javax.transaction.xa.XAResource.TMFAIL;
import static javax.transaction.xa.XAResource.TMNOFLAGS;
import static javax.transaction.xa.XAResource.TMRESUME;
import static javax.transaction.xa.XAResource.TMSUCCESS;
import static javax.transaction.xa.XAResource.TMSUSPEND;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.internal.transaction.xa.AbstractXAResourceManager;
import org.mule.runtime.core.internal.transaction.xa.AbstractXaTransactionContext;
import org.mule.runtime.core.internal.transaction.xa.DefaultXASession;

import org.junit.Before;
import org.junit.Test;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;


public class XaSessionTestCase {

  private AbstractXAResourceManager resourceManager = mock(AbstractXAResourceManager.class);
  private DefaultXASession xaSession;
  private static final AbstractXaTransactionContext TX_CONTEXT = mock(AbstractXaTransactionContext.class);

  @Before
  public void setUp() {
    xaSession = new TestXaSession(resourceManager);
  }

  @Test
  public void resource() {
    assertThat(xaSession.getXAResource(), is(xaSession));
    assertThat(xaSession.getResourceManager(), is(resourceManager));
  }

  @Test
  public void sameRM() throws XAException {
    assertThat(xaSession.isSameRM(xaSession), is(true));
    assertThat(xaSession.isSameRM(new TestXaSession(resourceManager)), is(true));
    assertThat(xaSession.isSameRM(new TestXaSession(mock(AbstractXAResourceManager.class))), is(false));
  }

  @Test
  public void startTx() throws Exception {
    Xid xid = mock(Xid.class);
    xaSession.start(xid, TMNOFLAGS);
    verify(resourceManager).beginTransaction(TX_CONTEXT);
    verify(resourceManager).addActiveTransactionalResource(xid, TX_CONTEXT);
  }

  @Test(expected = XAException.class)
  public void startTxAlreadyStarted() throws Exception {
    Xid xid = mock(Xid.class);
    xaSession.start(xid, TMNOFLAGS);
    xaSession.start(xid, TMNOFLAGS);
  }

  @Test(expected = XAException.class)
  public void startTxWithFailure() throws Exception {
    Xid xid = mock(Xid.class);
    doThrow(RuntimeException.class).when(resourceManager).beginTransaction(TX_CONTEXT);
    xaSession.start(xid, TMNOFLAGS);
  }

  @Test
  public void resumesTx() throws Exception {
    Xid xid = mock(Xid.class);
    when(resourceManager.getSuspendedTransactionalResource(xid)).thenReturn(TX_CONTEXT);
    xaSession.start(xid, TMRESUME);
    verify(resourceManager).removeSuspendedTransactionalResource(xid);
    verify(resourceManager).addActiveTransactionalResource(xid, TX_CONTEXT);
  }

  @Test(expected = XAException.class)
  public void endTxNotStarted() throws Exception {
    xaSession.end(mock(Xid.class), TMSUCCESS);
  }

  @Test
  public void endTx() throws Exception {
    Xid xid = mock(Xid.class);
    xaSession.start(xid, TMNOFLAGS);
    xaSession.end(xid, TMSUCCESS);
    verify(resourceManager, never()).addSuspendedTransactionalResource(xid, TX_CONTEXT);
    verify(resourceManager, never()).removeActiveTransactionalResource(xid);
    verify(resourceManager, never()).setTransactionRollbackOnly(TX_CONTEXT);
    // we can start it again and there is no error
    xaSession.start(xid, TMNOFLAGS);
  }

  @Test
  public void suspend() throws Exception {
    Xid xid = mock(Xid.class);
    xaSession.start(xid, TMNOFLAGS);
    xaSession.end(xid, TMSUSPEND);
    verify(resourceManager).addSuspendedTransactionalResource(xid, TX_CONTEXT);
    verify(resourceManager).removeActiveTransactionalResource(xid);
    verify(resourceManager, never()).setTransactionRollbackOnly(TX_CONTEXT);
    // we can start it again and there is no error
    xaSession.start(xid, TMNOFLAGS);
  }

  @Test
  public void rollbackOnly() throws Exception {
    Xid xid = mock(Xid.class);
    xaSession.start(xid, TMSUCCESS);
    xaSession.end(xid, TMFAIL);
    verify(resourceManager, never()).addSuspendedTransactionalResource(xid, TX_CONTEXT);
    verify(resourceManager, never()).removeActiveTransactionalResource(xid);
    verify(resourceManager).setTransactionRollbackOnly(TX_CONTEXT);
    // we can start it again and there is no error
    xaSession.start(xid, TMNOFLAGS);
  }

  @Test(expected = XAException.class)
  public void commitNoXid() throws Exception {
    xaSession.commit(null, false);
  }

  @Test
  public void commit() throws Exception {
    Xid xid = mock(Xid.class);
    when(resourceManager.getActiveTransactionalResource(xid)).thenReturn(TX_CONTEXT);
    xaSession.commit(xid, true);
    verify(resourceManager).commitTransaction(TX_CONTEXT);
  }

  @Test(expected = XAException.class)
  public void rollbackNoXid() throws Exception {
    xaSession.rollback(null);
  }

  @Test
  public void rollback() throws Exception {
    Xid xid = mock(Xid.class);
    when(resourceManager.getActiveTransactionalResource(xid)).thenReturn(TX_CONTEXT);
    xaSession.rollback(xid);
    verify(resourceManager).rollbackTransaction(TX_CONTEXT);
  }

  @Test
  public void prepare() throws Exception {
    Xid xid = mock(Xid.class);
    when(resourceManager.getTransactionalResource(xid)).thenReturn(TX_CONTEXT);
    xaSession.prepare(xid);
    verify(resourceManager).prepareTransaction(TX_CONTEXT);
  }

  @Test(expected = XAException.class)
  public void prepareNoContext() throws Exception {
    Xid xid = mock(Xid.class);
    xaSession.prepare(xid);
  }

  @Test(expected = XAException.class)
  public void prepareNoXid() throws Exception {
    xaSession.prepare(null);
  }

  @Test(expected = XAException.class)
  public void forgetNoContext() throws XAException {
    Xid xid = mock(Xid.class);
    xaSession.forget(xid);
  }

  @Test
  public void forget() throws XAException {
    Xid xid = mock(Xid.class);
    when(resourceManager.getTransactionalResource(xid)).thenReturn(TX_CONTEXT);
    xaSession.forget(xid);
    verify(resourceManager).removeSuspendedTransactionalResource(xid);
    verify(resourceManager).removeActiveTransactionalResource(xid);
  }

  @Test
  public void timeout() throws XAException {
    when(resourceManager.getDefaultTransactionTimeout()).thenReturn(1000L);
    assertThat(xaSession.getTransactionTimeout(), is(1));
  }

  private static final class TestXaSession extends DefaultXASession {

    protected TestXaSession(AbstractXAResourceManager resourceManager) {
      super(resourceManager);
    }

    @Override
    protected void commitDanglingTransaction(Xid xid, boolean onePhase) throws XAException {

    }

    @Override
    protected void rollbackDandlingTransaction(Xid xid) throws XAException {

    }

    @Override
    protected AbstractXaTransactionContext createTransactionContext(Xid xid) {
      return TX_CONTEXT;
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
      return new Xid[0];
    }
  }

}
