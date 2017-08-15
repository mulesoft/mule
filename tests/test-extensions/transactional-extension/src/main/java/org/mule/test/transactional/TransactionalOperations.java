/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transactional;

import static java.lang.System.identityHashCode;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;
import org.mule.test.transactional.connection.TestTransactionalConnection;

import java.util.List;
import java.util.Optional;

public class TransactionalOperations {

  @OutputResolver(output = TransactionalMetadataResolver.class)
  public TestTransactionalConnection getConnection(@Connection TestTransactionalConnection connection) {
    return connection;
  }

  public void verifyNoTransaction(@Connection TestTransactionalConnection connection) {
    checkState(!connection.isTransactionBegun(), "transaction begun with no reason");
  }

  public void verifyTransactionBegun(@Connection TestTransactionalConnection connection) {
    checkState(connection.isTransactionBegun(), "transaction not begun");
  }

  public void verifyTransactionCommited(@Connection TestTransactionalConnection connection) {
    checkState(connection.isTransactionCommited(), "transaction not committed");
  }

  public void verifyTransactionRolledback(@Connection TestTransactionalConnection connection) {
    checkState(connection.isTransactionRolledback(), "transaction not rolled back");
  }

  public void verifySameConnection(@Connection TestTransactionalConnection connection,
                                   @Content TestTransactionalConnection transactionalConnection) {
    checkArgument(transactionalConnection != null, "The transactionalConnection can't be null");
    checkState(connection.getConnectionId() == transactionalConnection.getConnectionId(), "The connection is not the same");
  }

  public OperationTransactionalAction injectTransactionalAction(@Connection TestTransactionalConnection connection,
                                                                OperationTransactionalAction action) {
    return action;
  }

  public PagingProvider<TestTransactionalConnection, Integer> pagedTransactionalOperation() throws Exception {
    return new PagingProvider<TestTransactionalConnection, Integer>() {

      private static final int SIZE = 2;
      private int count = 0;

      @Override
      public List<Integer> getPage(TestTransactionalConnection connection) {
        return count++ < SIZE ? asList(identityHashCode(connection)) : emptyList();
      }

      @Override
      public Optional<Integer> getTotalResults(TestTransactionalConnection connection) {
        return of(SIZE);
      }

      @Override
      public void close(TestTransactionalConnection connection) throws MuleException {

      }

      @Override
      public boolean useStickyConnections() {
        return false;
      }
    };
  }

  public void fail() {
    throw new RuntimeException("you better rollback!");
  }
}
