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
import static org.mockito.Mockito.verify;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Banana;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class PassThroughConnectionHandlerTestCase extends AbstractMuleTestCase {

  @Mock
  private Banana connection;

  @Mock
  private ConnectionProvider<Banana> connectionProvider;

  private ConnectionHandlerAdapter<Banana> managedConnection;

  @Before
  public void before() {
    managedConnection = new PassThroughConnectionHandler<>(connection, connectionProvider);
  }

  @Test
  public void getConnection() throws Exception {
    assertThat(managedConnection.getConnection(), is(sameInstance(connection)));
  }

  @Test
  public void alwaysReturnsSameConnection() throws Exception {
    getConnection();
    getConnection();
  }

  @Test
  public void release() throws Exception {
    doTwiceDisconnectOnce(managedConnection::release);
  }

  @Test
  public void invalidate() throws Exception {
    doTwiceDisconnectOnce(managedConnection::invalidate);
  }

  @Test
  public void getConnectionProvider() {
    assertThat(managedConnection.getConnectionProvider(), is(sameInstance(connectionProvider)));
  }

  @Test
  public void close() throws Exception {
    doTwiceDisconnectOnce(() -> {
      try {
        managedConnection.close();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private void doTwiceDisconnectOnce(Runnable task) {
    task.run();
    task.run();

    verify(connectionProvider).disconnect(connection);
  }
}
