/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener.grizzly;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import org.mule.service.http.api.server.ServerAddress;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.IOEvent;
import org.glassfish.grizzly.IOStrategy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExecutorPerServerAddressIOStrategyTestCase extends AbstractMuleTestCase {

  @Mock
  private ExecutorProvider executorProvider;
  @Mock
  private Connection connection;
  @Mock
  private Executor executor;

  private IOStrategy ioStrategy;

  @Before
  public void before() throws UnknownHostException {
    ioStrategy = new ExecutorPerServerAddressIOStrategy(executorProvider);
    when(connection.getLocalAddress()).thenReturn(new InetSocketAddress(InetAddress.getLocalHost(), 80));
    when(executorProvider.getExecutor(any(ServerAddress.class))).thenReturn(executor);
  }

  @Test
  public void serverAcceptIOEventDoesNotUseExecutor() {
    assertThat(ioStrategy.getThreadPoolFor(connection, IOEvent.SERVER_ACCEPT), is(nullValue()));
  }

  @Test
  public void readIOEventUsesExecutor() {
    assertThat(ioStrategy.getThreadPoolFor(connection, IOEvent.READ), is(equalTo(executor)));
  }

  @Test
  public void closeIOEventUsesExecutor() {
    assertThat(ioStrategy.getThreadPoolFor(connection, IOEvent.CLOSED), is(equalTo(executor)));
  }

}
