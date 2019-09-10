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
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.PoolingListener;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.apache.commons.pool.ObjectPool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class PoolingConnectionHandlerTestCase extends AbstractMuleTestCase {

  private static int DELAY = 1000;

  @Mock
  private ObjectPool<Object> pool;

  @Mock
  private Object config;

  @Mock
  private Object connection;

  @Mock
  private PoolingListener<Object> poolingListener;

  @Mock
  private ConnectionProvider connectionProvider;

  private PoolingConnectionHandler<Object> managedConnection;

  @Before
  public void before() {
    managedConnection = new PoolingConnectionHandler<>(connection, pool, poolingListener, connectionProvider);
    when(connectionProvider.validate(connection)).thenReturn(success());
  }

  @Test
  public void getConnection() throws Exception {
    assertThat(managedConnection.getConnection(), is(sameInstance(connection)));
  }

  @Test
  public void release() throws Exception {
    managedConnection.release();
    verify(pool).returnObject(connection);
    verify(poolingListener).onReturn(connection);
    assertDisconnected();
  }

  @Test
  public void releaseInvalidConnection() throws Exception {
    when(connectionProvider.validate(connection)).thenReturn(failure("Connection is invalid", new IOException()));
    managedConnection.release();
    verify(pool).invalidateObject(connection);
    verify(pool, never()).returnObject(connection);
    verify(poolingListener, never()).onReturn(connection);
  }

  @Test
  public void invalidate() throws Exception {
    managedConnection.invalidate();
    verify(pool).invalidateObject(connection);
    assertDisconnected();
  }

  @Test
  public void releaseConnectionTwice() throws Exception {
    when(connectionProvider.validate(connection)).thenAnswer((invocation) -> {
      Thread.sleep((int) (Math.random() * DELAY));
      return success();
    });

    Runnable release = () -> managedConnection.release();
    new Thread(release).start();
    new Thread(release).start();

    Thread.sleep(DELAY);
    verify(pool, times(0)).returnObject(null);
  }

  private void assertDisconnected() throws org.mule.runtime.api.connection.ConnectionException {
    try {
      managedConnection.getConnection();
      fail("Was expecting failure");
    } catch (IllegalStateException e) {
      // yeah baby!
    }
  }

  @Test
  public void close() throws Exception {
    managedConnection.close();
    verify(pool, never()).returnObject(anyObject());
  }

  @Test
  public void getConnectionProvider() {
    assertThat(managedConnection.getConnectionProvider(), is(sameInstance(connectionProvider)));
  }
}
