/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.internal.profiling.NoOpProfilingService;
import org.mule.runtime.core.privileged.transaction.TransactionConfig;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.module.extension.internal.runtime.transaction.TransactionSourceBinder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class DefaultSourceCallbackContextTestCase extends AbstractMuleTestCase {

  private ProfilingService profilingService = new NoOpProfilingService();

  @Test
  public void connectionReleasedOnTxExceptionNoConnHandler() throws ConnectionException, TransactionException {
    Object conn = mock(TransactionalConnection.class);

    final SourceCallbackAdapter sourceCallback = mock(SourceCallbackAdapter.class);

    final TransactionConfig txConfig = mock(TransactionConfig.class);
    when(txConfig.isTransacted()).thenReturn(true);
    when(sourceCallback.getTransactionConfig()).thenReturn(txConfig);

    final SourceConnectionManager sourceConnMgr = mock(SourceConnectionManager.class);
    when(sourceConnMgr.getConnectionHandler(conn)).thenReturn(empty());
    when(sourceCallback.getSourceConnectionManager()).thenReturn(sourceConnMgr);

    DefaultSourceCallbackContext ctx = new DefaultSourceCallbackContext(sourceCallback, profilingService, false);

    assertThrows(TransactionException.class, () -> ctx.bindConnection(conn));
    verify(sourceConnMgr).release(conn);
  }

  @Test
  public void connectionReleasedOnTxExceptionOnBindConnToTx() throws ConnectionException, TransactionException {
    Object conn = mock(TransactionalConnection.class);

    final SourceCallbackAdapter sourceCallback = mock(SourceCallbackAdapter.class);

    final TransactionConfig txConfig = mock(TransactionConfig.class);
    when(txConfig.isTransacted()).thenReturn(true);
    when(sourceCallback.getTransactionConfig()).thenReturn(txConfig);

    final SourceConnectionManager sourceConnMgr = mock(SourceConnectionManager.class);
    final ConnectionHandler connectionHandler = mock(ConnectionHandler.class);
    when(sourceConnMgr.getConnectionHandler(conn)).thenReturn(of(connectionHandler));
    when(sourceCallback.getSourceConnectionManager()).thenReturn(sourceConnMgr);

    final TransactionSourceBinder binder = mock(TransactionSourceBinder.class);
    when(binder.bindToTransaction(txConfig, sourceCallback.getConfigurationInstance(), sourceCallback.getSourceLocation(),
                                  connectionHandler, sourceCallback.getTimeout(), false))
        .thenThrow(TransactionException.class);
    when(sourceCallback.getTransactionSourceBinder()).thenReturn(binder);

    DefaultSourceCallbackContext ctx = new DefaultSourceCallbackContext(sourceCallback, profilingService, false);

    assertThrows(TransactionException.class, () -> ctx.bindConnection(conn));
    verify(sourceConnMgr).release(conn);
  }


}
