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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.MuleContext;
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

  @Mock
  private CachedConnectionProvider<Banana> connectionProvider;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private MuleContext muleContext;

  private DefaultConnectionManager connectionManager;

  @Before
  public void before() throws Exception {
    when(connectionProvider.connect()).thenReturn(connection);
    when(connectionProvider.validate(connection)).thenReturn(ConnectionValidationResult.success());

    connectionManager = new DefaultConnectionManager(muleContext);
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
}
