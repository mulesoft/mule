/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static java.lang.Thread.currentThread;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.tck.probe.PollingProber.probe;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.MuleContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Banana;

import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class CachedConnectionHandlerTestCase extends AbstractMuleTestCase {

  private final Banana connection = new Banana();

  @Mock
  private ConnectionProvider<Banana> connectionProvider;

  @Mock
  private Consumer<ConnectionHandlerAdapter<Banana>> releaser;

  @Mock
  private MuleContext muleContext;

  private CachedConnectionHandler<Banana> managedConnection;

  @Before
  public void before() throws Exception {
    stubConnectionProvider();
    managedConnection = new CachedConnectionHandler<>(connection, releaser, connectionProvider);
  }

  private void stubConnectionProvider() throws ConnectionException {
    reset(connectionProvider);
    when(connectionProvider.connect()).thenReturn(connection);
  }

  @Test
  public void getConnection() throws Exception {
    Banana connection = managedConnection.getConnection();
    verify(connectionProvider, never()).connect();
    assertThat(connection, is(sameInstance(this.connection)));
  }

  @Test
  public void returnsAlwaysSameConnectionAndConnectOnlyOnce() throws Exception {
    Banana connection1 = managedConnection.getConnection();
    Banana connection2 = managedConnection.getConnection();

    assertThat(connection1, is(sameInstance(connection2)));
    verify(connectionProvider, never()).connect();
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
  public void closeIsIdempotent() throws Exception {
    getConnection();
    managedConnection.close();
    verify(connectionProvider).disconnect(connection);

    reset(connectionProvider);
    managedConnection.close();

    verify(connectionProvider, never()).disconnect(connection);
  }

  @Test
  public void invalidate() throws Exception {
    getConnection();
    managedConnection.invalidate();
    verify(connectionProvider).disconnect(connection);
    verify(releaser).accept(managedConnection);

    before();
    getConnection();
  }

  @Test
  public void invalidateIsIdempotent() throws Exception {
    getConnection();
    managedConnection.invalidate();
    verify(connectionProvider).disconnect(connection);
    verify(releaser).accept(managedConnection);

    reset(connectionProvider, releaser);

    managedConnection.invalidate();
    verify(connectionProvider, never()).disconnect(connection);
    verify(releaser, never()).accept(managedConnection);
  }

  @Test
  public void concurrentInvalidate() throws Exception {
    for (int i = 0; i < 50; i++) {
      before();
      getConnection();
      Latch latch = new Latch();

      Thread t = new Thread(() -> {
        try {
          latch.await();
          managedConnection.invalidate();
        } catch (InterruptedException e) {
          currentThread().interrupt();
          throw new RuntimeException(e);
        }
      });

      t.start();
      try {
        latch.release();
        managedConnection.invalidate();

        probe(() -> {
          verify(connectionProvider).disconnect(connection);
          verify(releaser).accept(managedConnection);
          return true;
        });
      } finally {
        t.join();
      }
    }
  }

  @Test
  public void getConnectionProvider() {
    assertThat(managedConnection.getConnectionProvider(), is(sameInstance(connectionProvider)));
  }
}

