/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_CONNECTIONS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_ALWAYS_JOIN;
import static org.mule.runtime.module.extension.internal.util.ReconnectionUtils.shouldRetry;
import static org.mule.tck.util.MuleContextUtils.getNotificationDispatcher;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.transaction.XaTransaction;
import org.mule.runtime.extension.api.connectivity.XATransactionalConnection;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.operation.ExecutionContextConfigurationDecorator;
import org.mule.runtime.module.extension.internal.runtime.transaction.XAExtensionTransactionalResource;
import org.mule.runtime.tracer.api.component.ComponentTracer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Collection;
import java.util.Optional;
import java.util.Properties;

import javax.inject.Inject;
import javax.transaction.TransactionManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Issue;
import io.qameta.allure.Issues;

@RunWith(Parameterized.class)
public class ExtensionConnectionSupplierTestCase extends AbstractMuleContextTestCase {

  @Parameters(name = "lazyConnections: {0}")
  public static Collection<Boolean> params() {
    return asList(true, false);
  }

  @Rule
  public ExpectedException expected = none();

  @Inject
  private ExtensionConnectionSupplier adapter;

  @Inject
  private ConnectionManager connectionManager;

  private XaTransaction transaction;

  private Object config;

  private ExecutionContextAdapter operationContext;

  private ConnectionProvider connectionProvider;

  private ConfigurationInstance configurationInstance;

  private final boolean lazyConnections;


  public ExtensionConnectionSupplierTestCase(boolean lazyConnections) {
    this.lazyConnections = lazyConnections;
  }

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }

  @Override
  protected Optional<Properties> getDeploymentProperties() {
    final Properties deploymentProps = new Properties();
    deploymentProps.put(MULE_LAZY_CONNECTIONS_DEPLOYMENT_PROPERTY, Boolean.toString(lazyConnections));

    return of(deploymentProps);
  }

  @Before
  public void before() throws Exception {
    TransactionManager transactionManager = mock(TransactionManager.class, RETURNS_DEEP_STUBS);
    muleContext.setTransactionManager(transactionManager);
    transaction = spy(new XaTransaction("appName", transactionManager, getNotificationDispatcher(muleContext)));
    XATransactionalConnection connection = mock(XATransactionalConnection.class, RETURNS_DEEP_STUBS);
    config = new Object();

    operationContext = mock(ExecutionContextAdapter.class, RETURNS_DEEP_STUBS);
    connectionProvider = mock(ConnectionProvider.class);
    configurationInstance = mock(ConfigurationInstance.class);
    when(configurationInstance.getConnectionProvider()).thenReturn(of(connectionProvider));
    when(configurationInstance.getValue()).thenReturn(config);
    when(connectionProvider.connect()).thenReturn(connection);

    TransactionConfig transactionConfig = mock(TransactionConfig.class);
    when(transactionConfig.getAction()).thenReturn(ACTION_ALWAYS_JOIN);
    when(transactionConfig.isTransacted()).thenReturn(true);
    when(operationContext.getTransactionConfig()).thenReturn(of(transactionConfig));
    when(operationContext.getComponentModel()).thenReturn(mock(OperationModel.class));
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
    when(operationContext.getConfiguration()).thenReturn(of(configurationInstance));

    bindAndVerify();
  }

  @Test
  public void xaTransactionWithDecoratedConfig() throws Exception {
    ExecutionContextConfigurationDecorator configurationInstanceDecorator = mock(ExecutionContextConfigurationDecorator.class);
    when(configurationInstanceDecorator.getDecorated()).thenReturn(configurationInstance);
    when(configurationInstanceDecorator.getConnectionProvider()).thenReturn(of(connectionProvider));
    when(configurationInstanceDecorator.getValue()).thenReturn(config);

    when(operationContext.getConfiguration()).thenReturn(of(configurationInstanceDecorator));

    bindAndVerify();
  }

  @Test
  @Issues({@Issue("MULE-19288"), @Issue("MULE-17900")})
  public void xaTransactionNotSupports() throws Exception {
    when(operationContext.getConfiguration()).thenReturn(of(configurationInstance));
    when(transaction.supports(any(), any())).thenReturn(false);

    if (lazyConnections) {
      expected.expect(MuleRuntimeException.class);
      expected.expectCause(instanceOf(TransactionException.class));
      expected.expectMessage("but the current transaction doesn't support it and could not be bound");
    } else {
      expected.expect(TransactionException.class);
      expected.expectMessage("but the current transaction doesn't support it and could not be bound");
    }
    bindAndVerify();
  }

  @Test
  @Issues({@Issue("MULE-19288"), @Issue("MULE-17900")})
  public void xaTransactionBindResourceFails() throws Exception {
    when(operationContext.getConfiguration()).thenReturn(of(configurationInstance));

    final TransactionException txExceptionExpected = new TransactionException(new Exception());
    doThrow(txExceptionExpected).when(transaction).bindResource(any(), any());

    if (lazyConnections) {
      expected.expect(MuleRuntimeException.class);
      expected.expectCause(sameInstance(txExceptionExpected));
    } else {
      expected.expect(sameInstance(txExceptionExpected));
    }
    bindAndVerify();
  }

  @Test
  @Issue("MULE-19289")
  public void xaTransactionBindResourceFailsReleaseFails() throws Exception {
    when(operationContext.getConfiguration()).thenReturn(of(configurationInstance));

    final TransactionException txExceptionExpected = new TransactionException(new Exception());
    doThrow(txExceptionExpected).when(transaction).bindResource(any(), any());
    doThrow(IllegalStateException.class).when(connectionProvider).disconnect(any());

    if (lazyConnections) {
      expected.expect(MuleRuntimeException.class);
      expected.expectCause(sameInstance(txExceptionExpected));
    } else {
      expected.expect(sameInstance(txExceptionExpected));
    }
    bindAndVerify();
  }

  @Test
  @Issue("MULE-17900")
  public void transactionResourceBindWithLazyConnections() throws Exception {
    assumeThat(lazyConnections, is(true));
    when(operationContext.getConfiguration()).thenReturn(of(configurationInstance));
    when(connectionProvider.connect()).thenThrow(new AssertionError("Should not have tried to establish a connection"));

    connectionManager.bind(config, connectionProvider);
    TransactionCoordination.getInstance().bindTransaction(transaction);

    adapter.getConnection(operationContext, mock(ComponentTracer.class));
    verify(transaction, never()).bindResource(any(), any(XAExtensionTransactionalResource.class));
  }

  @Test
  @Issue("MULE-19347")
  public void ifXATransactionBindResourceFailsWithConnectionExceptionThenHandlerIsInvalidated() throws Exception {
    final ConnectionException expectedConnectionException =
        new ConnectionException("Failed to bind tx due to connectivity issue.");
    final TransactionException expectedTxException = new TransactionException(expectedConnectionException);

    assumeThat(lazyConnections, is(false));
    when(operationContext.getConfiguration()).thenReturn(of(configurationInstance));
    doThrow(expectedTxException).when(transaction).bindResource(any(), any());

    expected.expect(TransactionException.class);
    expected.expectCause(sameInstance(expectedConnectionException));

    try {
      connectionManager.bind(config, connectionProvider);
      TransactionCoordination.getInstance().bindTransaction(transaction);
      adapter.getConnection(operationContext, mock(ComponentTracer.class));
    } finally {
      verify(transaction).bindResource(any(), any(XAExtensionTransactionalResource.class));
      verify(connectionProvider).disconnect(any(XATransactionalConnection.class));
    }
  }

  private void bindAndVerify() throws TransactionException, ConnectionException {
    connectionManager.bind(config, connectionProvider);

    TransactionCoordination.getInstance().bindTransaction(transaction);

    final ConnectionHandler connection = adapter.getConnection(operationContext, mock(ComponentTracer.class));
    if (lazyConnections) {
      connection.getConnection();
    }
    verify(transaction).bindResource(any(), any(XAExtensionTransactionalResource.class));
  }

  @Test
  @Issue("MULE-19288")
  public void doNotRetryOnTxException() {
    when(operationContext.getConfiguration()).thenReturn(of(configurationInstance));

    final TransactionException txExceptionExpected = new TransactionException(new ConnectionException("expected!"));
    assertThat(shouldRetry(txExceptionExpected, operationContext), is(false));
  }
}
