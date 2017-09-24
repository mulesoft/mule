/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;

import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultConnectionManagerTestCase extends AbstractMuleTestCase {

  private Apple config = new Apple();

  private Banana connection = new Banana();

  private CachedConnectionProvider<Banana> connectionProvider;

  private ConnectionProvider<Banana> testeableConnectionProvider;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private MuleContextWithRegistries muleContext;

  private DefaultConnectionManager connectionManager;

  @Before
  public void before() throws Exception {
    connectionProvider = mockConnectionProvider(CachedConnectionProvider.class);
    testeableConnectionProvider = mockConnectionProvider(ConnectionProvider.class);
    when(muleContext.getRegistry().get(OBJECT_CONNECTION_MANAGER)).thenReturn(connectionManager);

    connectionManager = new DefaultConnectionManager(muleContext);
  }

  private <T extends ConnectionProvider> T mockConnectionProvider(Class<T> type) throws Exception {
    T connectionProvider = mock(type);
    when(connectionProvider.connect()).thenReturn(connection);
    when(connectionProvider.validate(connection)).thenReturn(success());

    return connectionProvider;
  }

  @Test
  public void getConnection() throws Exception {
    connectionManager.bind(config, connectionProvider);
    ConnectionHandler<Banana> connectionHandler = connectionManager.getConnection(config);
    assertThat(connectionHandler.getConnection(), is(sameInstance(connection)));
  }

  @Test(expected = ConnectionException.class)
  public void assertUnboundedConnection() throws Exception {
    connectionManager.getConnection(config);
  }

  @Test
  public void hasBinding() throws Exception {
    assertBound(false);
    connectionManager.bind(config, connectionProvider);
    assertBound(true);
    connectionManager.unbind(config);
    assertBound(false);
  }

  private void assertBound(boolean bound) {
    assertThat(connectionManager.hasBinding(config), is(bound));
  }

  @Test
  public void stop() throws Exception {
    getConnection();
    connectionManager.stop();
    verifyDisconnect();
  }

  @Test(expected = ConnectionException.class)
  public void unbind() throws Exception {
    getConnection();
    connectionManager.unbind(config);
    verifyDisconnect();
    assertUnboundedConnection();
  }

  private void verifyDisconnect() {
    verify(connectionProvider).disconnect(connection);
  }

  @Test(expected = IllegalStateException.class)
  public void bindWithStoppingMuleContext() throws Exception {
    when(muleContext.isStopped()).thenReturn(true);
    connectionManager.bind(config, connectionProvider);
  }

  @Test
  public void successfulConnectionProviderConnectivity() throws Exception {
    ConnectionValidationResult result = connectionManager.testConnectivity(testeableConnectionProvider);
    assertThat(result.isValid(), is(true));
    verify(testeableConnectionProvider).connect();
    verify(testeableConnectionProvider).validate(connection);
    verify(testeableConnectionProvider).disconnect(connection);
  }

  @Test
  public void failingConnectionProviderConnectivity() throws Exception {
    ConnectionValidationResult validationResult = failure("oops", new Exception());
    when(testeableConnectionProvider.validate(connection)).thenReturn(validationResult);
    ConnectionValidationResult result = connectionManager.testConnectivity(testeableConnectionProvider);
    assertThat(result, is(sameInstance(validationResult)));

    verify(testeableConnectionProvider).connect();
    verify(testeableConnectionProvider).validate(connection);
    verify(testeableConnectionProvider).disconnect(connection);
  }

  @Test
  public void poolingConnectionProviderConnectivity() throws Exception {
    testeableConnectionProvider = mockConnectionProvider(PoolingConnectionProvider.class);

    ConnectionValidationResult result = connectionManager.testConnectivity(testeableConnectionProvider);
    assertThat(result.isValid(), is(true));
    verify(testeableConnectionProvider).connect();
    verify(testeableConnectionProvider, atLeastOnce()).validate(connection);
    verify(testeableConnectionProvider, never()).disconnect(connection);
  }

  @Test
  public void cachedConnectionProviderConnectivity() throws Exception {
    ConnectionValidationResult result = connectionManager.testConnectivity(connectionProvider);
    assertThat(result.isValid(), is(true));
    verify(connectionProvider).connect();
    verify(connectionProvider, atLeastOnce()).validate(connection);
    verify(connectionProvider, never()).disconnect(connection);
  }

}
