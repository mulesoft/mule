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
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.core.privileged.util.LoggingTestUtils.verifyLogRegex;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.PoolingListener;
import org.mule.runtime.core.internal.logger.CustomLogger;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.apache.commons.pool2.impl.GenericObjectPool;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.slf4j.LoggerFactory;

@SmallTest
public class PoolingConnectionHandlerTestCase extends AbstractMuleTestCase {

  private static final CustomLogger logger = (CustomLogger) LoggerFactory.getLogger(PoolingConnectionHandler.class);

  private static int DELAY = 1000;
  private static final String poolId = "SomeConfigName-123";
  private static final String LOGGER_FIELD_NAME = "LOGGER";

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Mock
  private GenericObjectPool<Object> pool;

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
  public void before() throws Exception {
    managedConnection = new PoolingConnectionHandler<>(connection, pool, poolId, poolingListener, connectionProvider);
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

  @Test
  public void logReleaseConnection() {
    logger.resetLogs();
    managedConnection.release();
    verifyLogRegex(logger.getMessages(), "Returning back connection (.*) to pool {}", poolId);
  }

  @Test
  public void logInvalidateConnection() {
    logger.resetLogs();
    managedConnection.invalidate();
    verifyLogRegex(logger.getMessages(), "Invalidating connection (.*) from pool {}", poolId);
  }
}
