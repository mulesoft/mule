/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.module.extension.internal.runtime.transaction.TransactionSourceBinder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DefaultSourceCallbackContextTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  public void connectionReleasedOnTxExceptionNoConnHandler() throws ConnectionException, TransactionException {
    Object conn = mock(TransactionalConnection.class);

    final SourceCallbackAdapter sourceCallback = mock(SourceCallbackAdapter.class);

    final TransactionConfig txConfig = mock(TransactionConfig.class);
    when(txConfig.isTransacted()).thenReturn(true);
    when(sourceCallback.getTransactionConfig()).thenReturn(txConfig);

    final SourceConnectionManager sourceConnMgr = mock(SourceConnectionManager.class);
    when(sourceConnMgr.getConnectionHandler(conn)).thenReturn(Optional.empty());
    when(sourceCallback.getSourceConnectionManager()).thenReturn(sourceConnMgr);

    DefaultSourceCallbackContext ctx = new DefaultSourceCallbackContext(sourceCallback);

    expected.expect(TransactionException.class);
    try {
      ctx.bindConnection(conn);
    } finally {
      verify(sourceConnMgr).release(conn);
    }
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
    when(sourceConnMgr.getConnectionHandler(conn)).thenReturn(Optional.of(connectionHandler));
    when(sourceCallback.getSourceConnectionManager()).thenReturn(sourceConnMgr);

    final TransactionSourceBinder binder = mock(TransactionSourceBinder.class);
    when(binder.bindToTransaction(txConfig, sourceCallback.getConfigurationInstance(), sourceCallback.getSourceLocation(),
                                  connectionHandler, sourceCallback.getTransactionManager(), sourceCallback.getTimeout()))
                                      .thenThrow(TransactionException.class);
    when(sourceCallback.getTransactionSourceBinder()).thenReturn(binder);

    DefaultSourceCallbackContext ctx = new DefaultSourceCallbackContext(sourceCallback);

    expected.expect(TransactionException.class);
    try {
      ctx.bindConnection(conn);
    } finally {
      verify(sourceConnMgr).release(conn);
    }
  }


}
