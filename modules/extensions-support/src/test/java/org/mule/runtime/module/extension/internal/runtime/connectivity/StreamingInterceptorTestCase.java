/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_ALWAYS_BEGIN;
import static org.mule.runtime.core.api.execution.TransactionalExecutionTemplate.createTransactionalExecutionTemplate;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.CONNECTION_PARAM;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.transaction.ExtensionTransactionFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.test.petstore.extension.PetStoreClient;
import org.mule.test.petstore.extension.PetStoreConnectionProvider;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.petstore.extension.SimplePetStoreConnectionProvider;
import org.mule.test.petstore.extension.TransactionalPetStoreConnectionProvider;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StreamingInterceptorTestCase extends AbstractMuleContextTestCase {

  private static final String USER = "john";
  private static final String PASSWORD = "doe";

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExecutionContextAdapter operationContext;

  @Mock
  private ConfigurationInstance configurationInstance;

  @Mock
  private PetStoreConnector config;

  private PetStoreConnectionProvider<? extends PetStoreClient> connectionProvider;
  private ConnectionInterceptor interceptor;

  @Before
  public void before() throws Exception {

    when(operationContext.getConfiguration()).thenReturn(Optional.of(configurationInstance));
    when(configurationInstance.getValue()).thenReturn(config);
    setupConnectionProvider(new SimplePetStoreConnectionProvider());

    when(operationContext.getVariable(CONNECTION_PARAM)).thenReturn(null);
    when(operationContext.getTransactionConfig()).thenReturn(empty());

    interceptor = new ConnectionInterceptor();
    muleContext.getInjector().inject(interceptor);
  }

  private void setupConnectionProvider(PetStoreConnectionProvider<?> aConnectionProvider) throws Exception {
    this.connectionProvider = spy(aConnectionProvider);
    when(configurationInstance.getConnectionProvider()).thenReturn(Optional.of(connectionProvider));
    connectionProvider.setUsername(USER);
    connectionProvider.setPassword(PASSWORD);

    ConnectionManager connectionManager = muleContext.getRegistry().lookupObject(ConnectionManager.class);
    connectionManager.bind(config, connectionProvider);
  }


  @Test
  public void setConnection() throws Exception {
    PetStoreClient connection = getConnection();
    verify(connectionProvider).connect();

    assertThat(connection, is(notNullValue()));
    assertThat(connection.getUsername(), is(USER));
    assertThat(connection.getPassword(), is(PASSWORD));
  }

  @Test
  public void returnsAlwaysSameConnectionAndConnectOnlyOnce() throws Exception {
    PetStoreClient connection1 = getConnection();
    PetStoreClient connection2 = getConnection();

    assertThat(connection1, is(sameInstance(connection2)));
    verify(connectionProvider).connect();
  }

  @Test
  public void after() {
    ConnectionHandler connectionHandler = mock(ConnectionHandler.class);
    when(operationContext.removeVariable(CONNECTION_PARAM)).thenReturn(connectionHandler);

    interceptor.after(operationContext, null);
    verify(operationContext).removeVariable(CONNECTION_PARAM);
    verify(connectionHandler).release();
  }

  @Test
  public void alwaysReturnSameConnectionWhileInTransaction() throws Exception {
    setupConnectionProvider(new TransactionalPetStoreConnectionProvider());

    MuleTransactionConfig txConfig = new MuleTransactionConfig();
    txConfig.setFactory(new ExtensionTransactionFactory());
    txConfig.setAction(ACTION_ALWAYS_BEGIN);
    txConfig.setMuleContext(muleContext);
    when(operationContext.getTransactionConfig()).thenReturn(Optional.of(txConfig));

    createTransactionalExecutionTemplate(muleContext, txConfig).execute(() -> {
      PetStoreClient connection = getConnection();
      assertThat(connection, is(sameInstance(getConnection())));

      return null;
    });
  }

  private PetStoreClient getConnection() throws Exception {
    ArgumentCaptor<ConnectionHandler> connectionCaptor = ArgumentCaptor.forClass(ConnectionHandler.class);
    interceptor.before(operationContext);

    verify(operationContext, atLeastOnce()).setVariable(same(CONNECTION_PARAM), connectionCaptor.capture());
    ConnectionHandler<? extends PetStoreClient> connectionHandler = connectionCaptor.getValue();
    return connectionHandler.getConnection();
  }
}
