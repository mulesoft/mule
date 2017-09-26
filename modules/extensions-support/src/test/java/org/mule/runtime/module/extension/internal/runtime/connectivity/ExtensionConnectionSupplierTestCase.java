/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity;

import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_ALWAYS_JOIN;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.privileged.transaction.XaTransaction;
import org.mule.runtime.extension.api.connectivity.XATransactionalConnection;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.transaction.XAExtensionTransactionalResource;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import javax.inject.Inject;
import javax.transaction.TransactionManager;

public class ExtensionConnectionSupplierTestCase extends AbstractMuleContextTestCase {

  @Inject
  private ExtensionConnectionSupplier adapter;

  @Inject
  private ConnectionManager connectionManager;

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    Transaction transaction = TransactionCoordination.getInstance().getTransaction();
    if (transaction != null) {
      TransactionCoordination.getInstance().unbindTransaction(transaction);
    }
  }

  @Test
  public void xaTransaction() throws Exception {
    muleContext.setTransactionManager(mock(TransactionManager.class, RETURNS_DEEP_STUBS));
    XaTransaction transaction = spy(new XaTransaction(muleContext));
    XATransactionalConnection connection = mock(XATransactionalConnection.class, RETURNS_DEEP_STUBS);
    Object config = new Object();

    ExecutionContextAdapter operationContext = mock(ExecutionContextAdapter.class, RETURNS_DEEP_STUBS);
    ConnectionProvider connectionProvider = mock(ConnectionProvider.class);
    ConfigurationInstance configurationInstance = mock(ConfigurationInstance.class);
    when(configurationInstance.getConnectionProvider()).thenReturn(of(connectionProvider));
    when(operationContext.getConfiguration()).thenReturn(of(configurationInstance));
    when(configurationInstance.getValue()).thenReturn(config);
    when(connectionProvider.connect()).thenReturn(connection);

    TransactionConfig transactionConfig = mock(TransactionConfig.class);
    when(transactionConfig.getAction()).thenReturn(ACTION_ALWAYS_JOIN);
    when(transactionConfig.isTransacted()).thenReturn(true);
    when(operationContext.getTransactionConfig()).thenReturn(of(transactionConfig));

    connectionManager.bind(config, connectionProvider);

    TransactionCoordination.getInstance().bindTransaction(transaction);

    adapter.getConnection(operationContext);
    verify(transaction).bindResource(any(), any(XAExtensionTransactionalResource.class));
  }
}
