/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_CONNECTIONS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.tck.junit4.matcher.connection.ConnectionValidationResultFailureMatcher.isFailure;
import static org.mule.tck.junit4.matcher.connection.ConnectionValidationResultSuccessMatcher.isSuccess;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.LazyConnectionsStory.LAZY_CONNECTIONS;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.allure.AllureConstants.JavaSdk.ConnectivityTestingStory.CONNECTIVITY_TEST;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;

import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(JAVA_SDK)
@Story(CONNECTIVITY_TEST)
public class DefaultConnectionManagerTestCase extends AbstractMuleTestCase {

  private static final String CONNECTION_CREATION_FAILURE_MESSAGE = "Invalid credentials! Connection failed to create";

  private final Apple config = new Apple();

  private final Banana connection = new Banana();

  private CachedConnectionProvider<Banana> connectionProvider;

  private ConnectionProvider<Banana> testeableConnectionProvider;

  @Mock
  public ConfigurationInstance configurationInstance;

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock(answer = RETURNS_DEEP_STUBS)
  private MuleContextWithRegistry muleContext;

  private DefaultConnectionManager connectionManager;

  @Before
  public void before() throws Exception {
    connectionProvider = mockConnectionProvider(CachedConnectionProvider.class);
    testeableConnectionProvider = mockConnectionProvider(ConnectionProvider.class);
    when(muleContext.getRegistry().get(OBJECT_CONNECTION_MANAGER)).thenReturn(connectionManager);

    connectionManager = new DefaultConnectionManager(muleContext);
  }

  private <T extends ConnectionProvider> T mockConnectionProvider(Class<T> type) throws Exception {
    final T connectionProvider = mock(type);
    when(connectionProvider.connect()).thenReturn(connection);
    when(connectionProvider.validate(connection)).thenReturn(success());

    return connectionProvider;
  }

  @Test
  public void getConnection() throws Exception {
    connectionManager.bind(config, connectionProvider);
    final ConnectionHandler<Banana> connectionHandler = connectionManager.getConnection(config);
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
    when(muleContext.isStopping()).thenReturn(true);
    connectionManager.bind(config, connectionProvider);
  }

  @Test(expected = IllegalStateException.class)
  public void bindWithStoppedMuleContext() throws Exception {
    when(muleContext.isStopped()).thenReturn(true);
    connectionManager.bind(config, connectionProvider);
  }

  @Test
  public void bindWithStartingMuleContext() throws Exception {
    when(muleContext.isStopped()).thenReturn(true);
    when(muleContext.isStarting()).thenReturn(true);
    connectionManager.bind(config, connectionProvider);
    final ConnectionHandler<Banana> connectionHandler = connectionManager.getConnection(config);
    assertThat(connectionHandler.getConnection(), is(sameInstance(connection)));
  }

  @Test
  public void successfulConnectionProviderConnectivity() throws Exception {
    final ConnectionValidationResult result = connectionManager.testConnectivity(testeableConnectionProvider);

    assertThat(result, isSuccess());
    verify(testeableConnectionProvider).connect();
    verify(testeableConnectionProvider).validate(connection);
    verify(testeableConnectionProvider).disconnect(connection);
  }

  @Test
  public void failingConnectionProviderConnectivity() throws Exception {
    final ConnectionValidationResult validationResult = failure("oops", new Exception());
    when(testeableConnectionProvider.validate(connection)).thenReturn(validationResult);
    final ConnectionValidationResult result = connectionManager.testConnectivity(testeableConnectionProvider);
    assertThat(result, is(sameInstance(validationResult)));

    verify(testeableConnectionProvider).connect();
    verify(testeableConnectionProvider).validate(connection);
    verify(testeableConnectionProvider).disconnect(connection);
  }

  @Test
  public void poolingConnectionProviderConnectivity() throws Exception {
    testeableConnectionProvider = mockConnectionProvider(PoolingConnectionProvider.class);

    final ConnectionValidationResult result = connectionManager.testConnectivity(testeableConnectionProvider);
    assertThat(result, isSuccess());
    verify(testeableConnectionProvider).connect();
    verify(testeableConnectionProvider, atLeastOnce()).validate(connection);
    verify(testeableConnectionProvider, never()).disconnect(connection);
  }

  @Test
  public void cachedConnectionProviderConnectivity() throws Exception {
    final ConnectionValidationResult result = connectionManager.testConnectivity(connectionProvider);
    assertThat(result, isSuccess());
    verify(connectionProvider).connect();
    verify(connectionProvider, atLeastOnce()).validate(connection);
    verify(connectionProvider, never()).disconnect(connection);
  }

  @Test
  public void connectionProviderFailsToCreateConnectionOnConnectivityTest() throws Exception {
    when(testeableConnectionProvider.connect())
        .thenThrow(new ConnectionException(CONNECTION_CREATION_FAILURE_MESSAGE));
    final ConnectionValidationResult result = connectionManager.testConnectivity(testeableConnectionProvider);

    assertThat(result, isFailure(nullValue(ErrorType.class), is(CONNECTION_CREATION_FAILURE_MESSAGE)));
    assertThat(result.getException(), instanceOf(ConnectionException.class));

    verify(testeableConnectionProvider).connect();
    verify(testeableConnectionProvider, never()).validate(connection);
    verify(testeableConnectionProvider, never()).disconnect(connection);
  }

  @Test
  @Issue("W-14379073")
  @Story(LAZY_CONNECTIONS)
  public void forceConnectivityTestOnLazyConnections() throws Exception {
    final Properties deploymentProperties = new Properties();
    deploymentProperties.put(MULE_LAZY_CONNECTIONS_DEPLOYMENT_PROPERTY, "true");
    when(muleContext.getDeploymentProperties()).thenReturn(deploymentProperties);

    final DelegateConnectionManagerAdapter lazyConnectionManagerAdapter = new DelegateConnectionManagerAdapter(muleContext);

    when(testeableConnectionProvider.connect())
        .thenThrow(new ConnectionException(CONNECTION_CREATION_FAILURE_MESSAGE));
    doReturn(testeableConnectionProvider).when(configurationInstance).getConnectionProvider();
    final ConnectionValidationResult result = lazyConnectionManagerAdapter.testConnectivity(configurationInstance, true);

    assertThat(result, isFailure(nullValue(ErrorType.class), is(CONNECTION_CREATION_FAILURE_MESSAGE)));
    assertThat(result.getException(), instanceOf(ConnectionException.class));
  }

  @Test
  @Issue("W-14379073")
  @Story(LAZY_CONNECTIONS)
  public void forceConnectivityTestOnEagerConnections() throws Exception {
    final DelegateConnectionManagerAdapter lazyConnectionManagerAdapter = new DelegateConnectionManagerAdapter(muleContext);

    when(testeableConnectionProvider.connect())
        .thenThrow(new ConnectionException(CONNECTION_CREATION_FAILURE_MESSAGE));
    doReturn(testeableConnectionProvider).when(configurationInstance).getConnectionProvider();
    final ConnectionValidationResult result = lazyConnectionManagerAdapter.testConnectivity(configurationInstance);

    assertThat(result, isFailure(nullValue(ErrorType.class), is(CONNECTION_CREATION_FAILURE_MESSAGE)));
    assertThat(result.getException(), instanceOf(ConnectionException.class));
  }

}
