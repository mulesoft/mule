/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.tck.probe.PollingProber.probe;

import static java.lang.Thread.currentThread;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.extension.api.connectivity.XATransactionalConnection;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Banana;

import java.util.function.Consumer;

import javax.transaction.xa.XAResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@SmallTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class CachedConnectionHandlerTestCase extends AbstractMuleTestCase {

  private final Banana connection = new Banana();
  private final Banana legacyXaConnection = new LegacyXaBanana();

  @Mock
  private ConnectionProvider<Banana> connectionProvider;

  @Mock
  private ConnectionProvider<Banana> legacyXaConnectionProvider;

  @Mock
  private Consumer<ConnectionHandlerAdapter<Banana>> releaser;

  private CachedConnectionHandler<Banana> managedConnection;
  private CachedConnectionHandler<Banana> managedLegacyXaConnection;

  @BeforeEach
  public void before() throws Exception {
    stubConnectionProvider();
    managedConnection = new CachedConnectionHandler<>(connection, releaser, connectionProvider);
    managedLegacyXaConnection = new CachedConnectionHandler<>(legacyXaConnection, releaser, legacyXaConnectionProvider);
  }

  private void stubConnectionProvider() throws ConnectionException {
    reset(connectionProvider);
    reset(legacyXaConnectionProvider);
    when(connectionProvider.connect()).thenReturn(connection);
    when(legacyXaConnectionProvider.connect()).thenReturn(legacyXaConnection);
  }

  @Test
  void getConnection() throws Exception {
    Banana connection = managedConnection.getConnection();
    verify(connectionProvider, never()).connect();
    assertThat(connection, is(sameInstance(this.connection)));
  }

  @Test
  void getLegacyXaConnection() throws Exception {
    Banana connection = managedLegacyXaConnection.getConnection();
    verify(legacyXaConnectionProvider, never()).connect();
    assertThat(connection, is(sameInstance(this.legacyXaConnection)));
  }

  @Test
  void returnsAlwaysSameConnectionAndConnectOnlyOnce() throws Exception {
    Banana connection1 = managedConnection.getConnection();
    Banana connection2 = managedConnection.getConnection();

    assertThat(connection1, is(sameInstance(connection2)));
    verify(connectionProvider, never()).connect();
  }

  @Test
  void release() throws Exception {
    getConnection();
    managedConnection.release();
    verify(connectionProvider, never()).disconnect(connection);
  }

  @Test
  void releaseLegacyXa() throws Exception {
    getLegacyXaConnection();
    managedLegacyXaConnection.release();
    verify(legacyXaConnectionProvider, never()).disconnect(connection);
    assertThat(((LegacyXaBanana) legacyXaConnection).isClosed(), is(true));
  }

  @Test
  void close() throws Exception {
    getConnection();
    managedConnection.close();
    verify(connectionProvider).disconnect(connection);
  }

  @Test
  void closeIsIdempotent() throws Exception {
    getConnection();
    managedConnection.close();
    verify(connectionProvider).disconnect(connection);

    reset(connectionProvider);
    managedConnection.close();

    verify(connectionProvider, never()).disconnect(connection);
  }

  @Test
  void invalidate() throws Exception {
    getConnection();
    managedConnection.invalidate();
    verify(connectionProvider).disconnect(connection);
    verify(releaser).accept(managedConnection);

    before();
    getConnection();
  }

  @Test
  void invalidateIsIdempotent() throws Exception {
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
  void concurrentInvalidate() throws Exception {
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
  void getConnectionProvider() {
    assertThat(managedConnection.getConnectionProvider(), is(sameInstance(connectionProvider)));
  }

  private static final class LegacyXaBanana extends Banana implements XATransactionalConnection {

    private boolean closed;

    @Override
    public void begin() throws TransactionException {
      // TODO Auto-generated method stub

    }

    @Override
    public void commit() throws TransactionException {
      // TODO Auto-generated method stub

    }

    @Override
    public void rollback() throws TransactionException {
      // TODO Auto-generated method stub

    }

    @Override
    public XAResource getXAResource() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void close() {
      this.closed = true;
    }

    public boolean isClosed() {
      return closed;
    }
  }
}

