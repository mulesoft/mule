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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class NullConnectionManagementStrategyTestCase extends AbstractMuleTestCase {

  @Mock
  private ConnectionProvider<Banana> connectionProvider;

  @Mock
  private Apple config;

  @Mock
  private Banana connection;

  @Mock
  private MuleContext muleContext;

  private NullConnectionManagementStrategy<Banana> strategy;

  @Before
  public void before() throws Exception {
    when(connectionProvider.connect()).thenReturn(connection);
    strategy = new NullConnectionManagementStrategy<>(connectionProvider, muleContext);
  }

  @Test
  public void getConnection() throws Exception {
    ConnectionHandler<Banana> connectionHandler = strategy.getConnectionHandler();
    assertThat(connectionHandler.getConnection(), is(sameInstance(connection)));
  }

  @Test
  public void close() throws Exception {
    strategy.close();
    verify(connectionProvider, never()).disconnect(any(Banana.class));
  }
}
