/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transactional;

import static java.util.concurrent.Executors.newFixedThreadPool;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.test.transactional.connection.TestTransactionalConnection;

@MetadataScope(outputResolver = TransactionalMetadataResolver.class)
public class TransactionalSource extends Source<TestTransactionalConnection, Object> {

  @Connection
  private ConnectionProvider<TestTransactionalConnection> connectionProvider;

  public TransactionalSource() {
    isSuccess = null;
  }

  public static Boolean isSuccess;

  @Override
  public void onStart(SourceCallback<TestTransactionalConnection, Object> sourceCallback) throws MuleException {
    newFixedThreadPool(1).execute(() -> {
      SourceCallbackContext ctx = sourceCallback.createContext();
      try {
        TestTransactionalConnection connection = connectionProvider.connect();
        ctx.bindConnection(connection);
        sourceCallback.handle(Result.<TestTransactionalConnection, Object>builder().output(connection).build(), ctx);
      } catch (Exception e) {
        sourceCallback.onSourceException(e);
      }
    });
  }

  @Override
  public void onStop() {

  }

  @OnSuccess
  public void onSuccess() throws InterruptedException {
    isSuccess = true;
  }

  @OnError
  public void onError() {
    isSuccess = false;
  }

  @OnTerminate
  public void onTerminate() {

  }

}
