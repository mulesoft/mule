/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.transactional;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.tx.SourceTransactionalAction;
import org.mule.test.transactional.connection.TestTransactionalConnection;

import java.util.function.Function;

public class TransactionalSourceWithTXParameters extends Source<SourceTransactionalAction, Object> {

  @Connection
  private ConnectionProvider<TestTransactionalConnection> connection;

  @Parameter
  private SourceTransactionalAction txAction;

  public static Function<Object, Object> responseCallback;

  @Override
  public void onStart(SourceCallback<SourceTransactionalAction, Object> sourceCallback) throws MuleException {
    responseCallback.apply(txAction);
  }

  @Override
  public void onStop() {

  }
}
