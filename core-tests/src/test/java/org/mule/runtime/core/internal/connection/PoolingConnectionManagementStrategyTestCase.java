/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyVararg;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.config.PoolingProfile.DEFAULT_MAX_POOL_ACTIVE;
import static org.mule.runtime.api.config.PoolingProfile.DEFAULT_MAX_POOL_IDLE;
import static org.mule.runtime.api.config.PoolingProfile.DEFAULT_MAX_POOL_WAIT;
import static org.mule.runtime.api.config.PoolingProfile.DEFAULT_POOL_EXHAUSTED_ACTION;
import static org.mule.runtime.api.config.PoolingProfile.DEFAULT_POOL_INITIALISATION_POLICY;
import static org.mule.runtime.api.config.PoolingProfile.INITIALISE_ALL;
import static org.mule.runtime.api.config.PoolingProfile.INITIALISE_NONE;
import static org.mule.runtime.api.config.PoolingProfile.WHEN_EXHAUSTED_FAIL;
import static org.mule.runtime.api.config.PoolingProfile.WHEN_EXHAUSTED_WAIT;
import static org.mule.runtime.core.privileged.util.LoggingTestUtils.createMockLogger;
import static org.mule.runtime.core.privileged.util.LoggingTestUtils.setLogger;
import static org.mule.runtime.core.privileged.util.LoggingTestUtils.verifyLogRegex;
import static org.mule.tck.MuleTestUtils.spyInjector;
import static org.slf4j.event.Level.DEBUG;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingListener;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.Injector;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class PoolingConnectionManagementStrategyTestCase extends AbstractMuleContextTestCase {

  private static final int MAX_ACTIVE = 2;
  private static final String ownerConfigName = "SomeConfigName";
  private static final String LOGGER_FIELD_NAME = "LOGGER";

  private ConnectionProvider<Object> connectionProvider;

  private PoolingProfile poolingProfile =
      new PoolingProfile(MAX_ACTIVE, MAX_ACTIVE, DEFAULT_MAX_POOL_WAIT, WHEN_EXHAUSTED_WAIT, INITIALISE_NONE);
  private PoolingConnectionManagementStrategy<Object> strategy;
  private PoolingListener<Object> poolingListener;
  private Injector injector;

  private ConnectionHandler<Object> connection1;
  private ConnectionHandler<Object> connection2;
  private List<String> debugMessages;
  protected Logger logger;
  private Logger oldLogger;

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Before
  public void before() throws Exception {
    poolingListener = mock(PoolingListener.class);
    injector = spyInjector(muleContext);
    muleContext.start();
    resetConnectionProvider();
    debugMessages = new ArrayList<>();
    logger = createMockLogger(debugMessages, DEBUG);
    oldLogger = setLogger(PoolingConnectionManagementStrategy.class, LOGGER_FIELD_NAME, logger);
  }

  @After
  public void restoreLogger() throws Exception {
    setLogger(PoolingConnectionManagementStrategy.class, LOGGER_FIELD_NAME, oldLogger);
  }

  @Test
  public void getConnection() throws Exception {
    initStrategy();
    connection1 = strategy.getConnectionHandler();
    connection2 = strategy.getConnectionHandler();

    assertThat(connection1, is(not(sameInstance(connection2))));
    assertThat(connection1.getConnection(), is(not(sameInstance(connection2.getConnection()))));
    verify(connectionProvider, times(2)).connect();

    verify(poolingListener).onBorrow(connection1.getConnection());
    verify(poolingListener).onBorrow(connection2.getConnection());
  }

  @Test
  public void poolingListenerFailsOnBorrow() throws Exception {
    initStrategy();
    final RuntimeException exception = new RuntimeException();

    doThrow(exception).when(poolingListener).onBorrow(any(Lifecycle.class));

    try {
      strategy.getConnectionHandler();
      fail("was expecting poolingListener to fail");
    } catch (Exception e) {
      assertThat(e.getCause(), is(sameInstance(exception)));
      verify(connectionProvider).disconnect(any(Lifecycle.class));
    }
  }

  @Test
  public void connectionsDependenciesInjected() throws Exception {
    initStrategy();
    connection1 = strategy.getConnectionHandler();
    connection2 = strategy.getConnectionHandler();

    verify(injector).inject(connection1.getConnection());
    verify(injector).inject(connection2.getConnection());
  }

  @Test
  public void exhaustion() throws Exception {
    poolingProfile = new PoolingProfile(1, 1, DEFAULT_MAX_POOL_WAIT, WHEN_EXHAUSTED_FAIL, INITIALISE_NONE);
    initStrategy();

    ConnectionHandler<Object> connectionHandler = strategy.getConnectionHandler();
    try {
      strategy.getConnectionHandler();
      fail("Was expecting the pool to be exhausted");
    } catch (ConnectionException e) {
      // wiiiii
    }

    connectionHandler.release();
    assertThat(strategy.getConnectionHandler(), is(notNullValue()));
  }

  @Test
  public void release() throws Exception {
    initStrategy();
    connection1 = strategy.getConnectionHandler();
    connection2 = strategy.getConnectionHandler();

    connection1.release();
    connection2.release();

    strategy.close();

    verify(connectionProvider, times(2)).disconnect(any(Object.class));
  }

  @Test(expected = ConnectionException.class)
  public void failDueToInvalidConnection() throws ConnectionException {
    initStrategy();
    connection1 = strategy.getConnectionHandler();
    connection2 = strategy.getConnectionHandler();

    when(connectionProvider.validate(anyVararg())).thenReturn(ConnectionValidationResult
        .failure("Invalid username or password",
                 new Exception("401: UNAUTHORIZED")));
    strategy.getConnectionHandler().getConnection();
  }

  @Test(expected = ConnectionException.class)
  public void failDueToNullConnectionValidationResult() throws ConnectionException {
    initStrategy();
    connection1 = strategy.getConnectionHandler();
    connection2 = strategy.getConnectionHandler();

    when(connectionProvider.validate(anyVararg())).thenReturn(null);
    strategy.getConnectionHandler().getConnection();
  }

  @Test
  public void defaultInitializationPolicyIsHonored() throws ConnectionException {
    poolingProfile =
        new PoolingProfile(5, -1, DEFAULT_MAX_POOL_WAIT, DEFAULT_POOL_EXHAUSTED_ACTION, DEFAULT_POOL_INITIALISATION_POLICY);
    initStrategy();
    verifyConnections(1);
  }

  @Test
  public void initializationPolicyInitialiseAllWithUnlimitedMaxIdle() throws ConnectionException {
    poolingProfile = new PoolingProfile(5, -1, DEFAULT_MAX_POOL_WAIT, DEFAULT_POOL_EXHAUSTED_ACTION, INITIALISE_ALL);
    initStrategy();
    verifyConnections(5);
  }

  @Test
  public void initializationPolicyInitialiseAllWithUnlimitedMaxActive() throws ConnectionException {
    poolingProfile = new PoolingProfile(-1, 2, DEFAULT_MAX_POOL_WAIT, DEFAULT_POOL_EXHAUSTED_ACTION, INITIALISE_ALL);
    initStrategy();
    verifyConnections(2);
  }

  @Test
  public void initializationPolicyInitialiseAllWithUnlimitedMaxActiveAndUnlimitedMaxIdle() throws ConnectionException {
    poolingProfile = new PoolingProfile(-1, -1, DEFAULT_MAX_POOL_WAIT, DEFAULT_POOL_EXHAUSTED_ACTION, INITIALISE_ALL);
    initStrategy();
    verifyConnections(0);
  }

  @Test
  public void initializationPolicyInitialiseAllWithMaxIdleSmallerThanMaxActive() throws ConnectionException {
    poolingProfile = new PoolingProfile(5, 3, DEFAULT_MAX_POOL_WAIT, DEFAULT_POOL_EXHAUSTED_ACTION, INITIALISE_ALL);
    initStrategy();
    verifyConnections(3);
  }

  @Test
  public void initializationPolicyInitialiseAllWithMaxIdleGreaterThanMaxActive() throws ConnectionException {
    poolingProfile = new PoolingProfile(5, 8, DEFAULT_MAX_POOL_WAIT, DEFAULT_POOL_EXHAUSTED_ACTION, INITIALISE_ALL);
    initStrategy();
    verifyConnections(5);
  }

  @Test
  public void initializationPolicyInitialiseNone() throws ConnectionException {
    poolingProfile =
        new PoolingProfile(DEFAULT_MAX_POOL_ACTIVE, -1, DEFAULT_MAX_POOL_WAIT, DEFAULT_POOL_EXHAUSTED_ACTION, INITIALISE_NONE);
    initStrategy();
    verifyConnections(0);
  }

  @Test
  public void initializationDoesNotFailWhenEncounteringConnectionException() throws ConnectionException {
    ConnectionProvider<Object> connectionProvider = mock(ConnectionProvider.class);
    when(connectionProvider.connect()).thenReturn(new Object()).thenThrow(ConnectionException.class).thenReturn(new Object());
    this.connectionProvider = spy(new DefaultConnectionProviderWrapper<>(connectionProvider, muleContext));
    poolingProfile = new PoolingProfile(3, -1, DEFAULT_MAX_POOL_WAIT, DEFAULT_POOL_EXHAUSTED_ACTION, INITIALISE_ALL);
    initStrategy();
    verifyConnections(3);
  }

  @Test
  public void logInitialization() {
    poolingProfile =
        new PoolingProfile(DEFAULT_MAX_POOL_ACTIVE, DEFAULT_MAX_POOL_IDLE, DEFAULT_MAX_POOL_WAIT, WHEN_EXHAUSTED_FAIL,
                           DEFAULT_POOL_INITIALISATION_POLICY);
    initStrategy();
    verifyLogRegex(debugMessages, "Creating pool with ID (.*) for config {}", ownerConfigName);
    verifyLogRegex(debugMessages, "Initializing pool (.*) with {} initial connections", DEFAULT_POOL_INITIALISATION_POLICY);
    verifyLogRegex(debugMessages, "Created connection (.*)");
    verifyLogRegex(debugMessages,
                   "Status for pool (.*): 0 connections are active out of {} max active limit, {} connections are idle out of {} max idle limit",
                   DEFAULT_MAX_POOL_ACTIVE, DEFAULT_POOL_INITIALISATION_POLICY, DEFAULT_MAX_POOL_IDLE);
  }

  @Test
  public void logBorrowConnection() throws ConnectionException {
    poolingProfile =
        new PoolingProfile(DEFAULT_MAX_POOL_ACTIVE, DEFAULT_MAX_POOL_IDLE, DEFAULT_MAX_POOL_WAIT, DEFAULT_POOL_EXHAUSTED_ACTION,
                           DEFAULT_POOL_INITIALISATION_POLICY);
    initStrategy();
    connection1 = strategy.getConnectionHandler();
    verifyLogRegex(debugMessages, "Acquiring connection (.*) from the pool (.*)");
  }

  @Test
  public void logDestroyConnection() throws ConnectionException {
    poolingProfile =
        new PoolingProfile(DEFAULT_MAX_POOL_ACTIVE, DEFAULT_MAX_POOL_IDLE, DEFAULT_MAX_POOL_WAIT, DEFAULT_POOL_EXHAUSTED_ACTION,
                           DEFAULT_POOL_INITIALISATION_POLICY);
    initStrategy();
    connection1 = strategy.getConnectionHandler();
    connection1.invalidate();
    verifyLogRegex(debugMessages, "Disconnecting connection (.*)");
  }

  @Test
  public void logClosePool() throws MuleException {
    poolingProfile =
        new PoolingProfile(DEFAULT_MAX_POOL_ACTIVE, DEFAULT_MAX_POOL_IDLE, DEFAULT_MAX_POOL_WAIT, DEFAULT_POOL_EXHAUSTED_ACTION,
                           DEFAULT_POOL_INITIALISATION_POLICY);
    initStrategy();
    strategy.close();
    verifyLogRegex(debugMessages, "Closing pool (.*)");
  }

  @Test
  public void logPoolStatusUnlimited() throws ConnectionException {
    poolingProfile =
        new PoolingProfile(-1, -1, DEFAULT_MAX_POOL_WAIT, DEFAULT_POOL_EXHAUSTED_ACTION, DEFAULT_POOL_INITIALISATION_POLICY);
    initStrategy();
    verifyLogRegex(debugMessages,
                   "Status for pool (.*): 0 connections are active out of unlimited max active limit, {} connections are idle out of unlimited max idle limit",
                   DEFAULT_POOL_INITIALISATION_POLICY);
  }

  private void resetConnectionProvider() throws ConnectionException {
    ConnectionProvider<Object> connectionProvider = mock(ConnectionProvider.class);
    when(connectionProvider.connect()).thenAnswer(i -> mock(Lifecycle.class));
    when(connectionProvider.validate(anyObject())).thenReturn(ConnectionValidationResult.success());
    this.connectionProvider = spy(new DefaultConnectionProviderWrapper<>(connectionProvider, muleContext));
  }

  private void initStrategy() {
    strategy = new PoolingConnectionManagementStrategy<>(connectionProvider, poolingProfile, poolingListener, muleContext,
                                                         ownerConfigName);
  }

  private void verifyConnections(int numToCreate) throws ConnectionException {
    verify(this.connectionProvider, times(numToCreate)).connect();
    verify(this.connectionProvider, times(0)).disconnect(any());
  }
}
