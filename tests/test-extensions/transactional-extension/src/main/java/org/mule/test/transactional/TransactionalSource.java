/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transactional;

import static java.util.concurrent.Executors.newFixedThreadPool;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.api.tx.TransactionType;
import org.mule.runtime.core.api.util.concurrent.NamedThreadFactory;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.connectivity.XATransactionalConnection;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.extension.api.tx.TransactionHandle;
import org.mule.test.transactional.connection.DummyXaResource;
import org.mule.test.transactional.connection.TestTransactionalConnection;
import org.mule.test.transactional.connection.TestXaTransactionalConnection;

import java.util.concurrent.ExecutorService;

@MetadataScope(outputResolver = TransactionalMetadataResolver.class)
public class TransactionalSource extends Source<TestTransactionalConnection, Object> {

  private static final String IS_XA = "isXa";

  public static Boolean isSuccess;
  public static DummyXaResource xaResource;

  @Parameter
  TransactionType txType;

  @Connection
  private ConnectionProvider<TestTransactionalConnection> connectionProvider;

  private ExecutorService connectExecutor;

  public TransactionalSource() {
    isSuccess = null;
    xaResource = null;
  }

  @Override
  public void onStart(SourceCallback<TestTransactionalConnection, Object> sourceCallback) {
    connectExecutor = newFixedThreadPool(1, new NamedThreadFactory(TransactionalSource.class.getName()));
    connectExecutor.execute(() -> {
      SourceCallbackContext ctx = sourceCallback.createContext();
      TransactionHandle txHandle = null;
      try {
        TestTransactionalConnection connection = connectionProvider.connect();

        boolean isXa = false;
        if (connection instanceof XATransactionalConnection) {
          isXa = true;
          xaResource = (DummyXaResource) ((XATransactionalConnection) connection).getXAResource();
        }

        ctx.addVariable(IS_XA, isXa);
        txHandle = ctx.bindConnection(connection);
        sourceCallback.handle(Result.<TestTransactionalConnection, Object>builder().output(connection).build(), ctx);
      } catch (ConnectionException e) {
        sourceCallback.onConnectionException(e);
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        try {
          if (txHandle != null) {
            txHandle.commit();
          }
        } catch (TransactionException e) {
          try {
            txHandle.rollback();
          } catch (TransactionException e1) {
            final RuntimeException rollbackException = new RuntimeException(e1);
            rollbackException.addSuppressed(e);
            throw rollbackException;
          }
          throw new RuntimeException(e);
        }
      }
    });
  }

  @Override
  public void onStop() {
    connectExecutor.shutdownNow();
    connectExecutor = null;
  }

  @OnSuccess
  public void onSuccess(org.mule.sdk.api.runtime.source.SourceCallbackContext ctx)
      throws TransactionException {
    ctx.getTransactionHandle().commit();
    isSuccess = true;
  }

  @OnError
  public void onError(SourceCallbackContext ctx)
      throws TransactionException {
    ctx.getTransactionHandle().rollback();
    isSuccess = false;
  }

  @OnTerminate
  public void onTerminate(org.mule.sdk.api.runtime.source.SourceCallbackContext ctx) {
    Boolean isXa = (Boolean) ctx.getVariable(IS_XA).get();
    if (isXa) {
      TestXaTransactionalConnection connection = ctx.getConnection();
      DummyXaResource xaResource = (DummyXaResource) connection.getXAResource();
    }
  }
}
