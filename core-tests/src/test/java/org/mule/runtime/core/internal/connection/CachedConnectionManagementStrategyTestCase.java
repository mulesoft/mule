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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.MuleContext;
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
public class CachedConnectionManagementStrategyTestCase extends AbstractMuleTestCase {

  private Banana connection = new Banana();

  @Mock
  private ConnectionProvider<Banana> connectionProvider;

  @Mock
  private MuleContext muleContext;

  private CachedConnectionManagementStrategy<Banana> connectionStrategy;

  @Before
  public void before() throws Exception {
    when(connectionProvider.connect()).thenReturn(connection);
    connectionStrategy = new CachedConnectionManagementStrategy<>(connectionProvider, muleContext);
    when(connectionProvider.validate(connection)).thenReturn(ConnectionValidationResult.success());
  }

  @Test
  public void getConnection() throws Exception {
    ConnectionHandler<Banana> connectionHandler = connectionStrategy.getConnectionHandler();

    Banana connection = connectionHandler.getConnection();
    verify(connectionProvider).connect();
    assertThat(connection, is(sameInstance(this.connection)));
  }

  @Test
  public void getConnectionIsIdempotent() throws Exception {
    getConnection();

    reset(connectionProvider);

    Banana connection = connectionStrategy.getConnectionHandler().getConnection();
    verify(connectionProvider, never()).connect();
    assertThat(connection, is(sameInstance(this.connection)));
  }

  @Test
  public void close() throws Exception {
    connectionStrategy.getConnectionHandler().getConnection();
    connectionStrategy.close();
    verify(connectionProvider).disconnect(connection);
  }

  @Test
  public void getConnectionConcurrentlyAndNeverConnect() throws Exception {
    Latch latch = new Latch();
    when(connectionProvider.connect()).thenAnswer(invocation -> {
      new Thread(() -> {
        try {
          latch.release();
          getConnection();
        } catch (Exception e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
      }).start();

      return connection;
    });

    Banana connection = connectionStrategy.getConnectionHandler().getConnection();
    assertThat(latch.await(5, TimeUnit.SECONDS), is(true));
    assertThat(connection, is(sameInstance(this.connection)));
    verify(connectionProvider).connect();
  }
}
