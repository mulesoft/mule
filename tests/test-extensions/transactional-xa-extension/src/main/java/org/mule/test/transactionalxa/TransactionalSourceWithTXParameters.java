/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transactionalxa;

import org.mule.runtime.api.exception.MuleException;
import org.mule.sdk.api.annotation.param.Connection;
import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.connectivity.ConnectionProvider;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;
import org.mule.sdk.api.tx.SourceTransactionalAction;
import org.mule.test.transactionalxa.connection.TestTransactionalConnection;

import java.util.function.Function;

public class TransactionalSourceWithTXParameters extends Source<SourceTransactionalAction, Object> {

  @Connection
  private ConnectionProvider<TestTransactionalConnection> connection;

  @Parameter
  private SourceTransactionalAction transactionalAction;

  public static Function<Object, Object> responseCallback;

  @Override
  public void onStart(SourceCallback<SourceTransactionalAction, Object> sourceCallback) throws MuleException {
    responseCallback.apply(transactionalAction);
  }

  @Override
  public void onStop() {

  }
}
