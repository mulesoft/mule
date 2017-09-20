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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Banana;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class CachedConnectionHandlerTestCase extends AbstractMuleTestCase {

  private Banana connection = new Banana();

  @Mock
  private ConnectionProvider<Banana> connectionProvider;

  @Mock
  private MuleContext muleContext;

  private CachedConnectionHandler<Banana> managedConnection;

  @Before
  public void before() throws Exception {
    stubConnectionProvider();
    managedConnection = new CachedConnectionHandler<>(connectionProvider, muleContext);
  }

  private void stubConnectionProvider() throws org.mule.runtime.api.connection.ConnectionException {
    when(connectionProvider.connect()).thenReturn(connection);
    when(connectionProvider.validate(connection)).thenReturn(success());
  }

  @Test
  public void getConnection() throws Exception {
    Banana connection = managedConnection.getConnection();
    verify(connectionProvider).connect();
    assertThat(connection, is(sameInstance(connection)));
  }

  @Test
  public void returnsAlwaysSameConnectionAndConnectOnlyOnce() throws Exception {
    Banana connection1 = managedConnection.getConnection();
    Banana connection2 = managedConnection.getConnection();

    assertThat(connection1, is(sameInstance(connection2)));
    verify(connectionProvider).connect();
  }

  @Test
  public void getConnectionConcurrentlyAndConnectOnlyOnce() throws Exception {
    Banana mockConnection = mock(Banana.class);
    connectionProvider = mock(ConnectionProvider.class);
    before();

    Latch latch = new Latch();
    when(connectionProvider.connect()).thenAnswer(invocation -> {
      new Thread(() -> {
        try {
          latch.release();
          getConnection();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).start();

      return mockConnection;
    });

    Banana connection = managedConnection.getConnection();
    assertThat(latch.await(5, TimeUnit.SECONDS), is(true));
    assertThat(connection, is(sameInstance(mockConnection)));
    verify(connectionProvider).connect();
  }

  @Test
  public void release() throws Exception {
    getConnection();
    managedConnection.release();
    verify(connectionProvider, never()).disconnect(connection);
  }

  @Test
  public void close() throws Exception {
    getConnection();
    managedConnection.close();
    verify(connectionProvider).disconnect(connection);
  }

  @Test
  public void invalidate() throws Exception {
    getConnection();
    managedConnection.invalidate();
    verify(connectionProvider).disconnect(connection);
    reset(connectionProvider);
    stubConnectionProvider();
    getConnection();
  }

  @Test
  public void getConnectionProvider() {
    assertThat(managedConnection.getConnectionProvider(), is(sameInstance(connectionProvider)));
  }
}

