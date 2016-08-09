/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.request.grizzly;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.ning.http.client.AsyncHandler;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.IOEvent;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.attributes.AttributeHolder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class FlowWorkManagerIOStrategyTestCase extends AbstractMuleTestCase {

  FlowWorkManagerIOStrategy ioStrategy = FlowWorkManagerIOStrategy.getInstance();

  @Mock
  Connection connection;
  @Mock
  AsyncHandler asyncHandler;
  @Mock
  ExecutorService grizzlyWorkerThreadPool;
  @Mock
  WorkManager flowWorkManager;

  @Before
  public void setup() {
    AttributeHolder attributeHolder = mock(AttributeHolder.class);
    when(connection.getAttributes()).thenReturn(attributeHolder);
    Transport transport = mock(Transport.class);
    when(connection.getTransport()).thenReturn(transport);
    when(transport.getWorkerThreadPool()).thenReturn(grizzlyWorkerThreadPool);
  }


  @Test
  public void flowWorkManagerUsedForReadIOEvent() throws IOException {
    new TestFlowWorkManagerIOStrategy(flowWorkManager).executeIoEvent(connection, IOEvent.READ);
    verify(flowWorkManager, times(1)).execute(any(Runnable.class));
  }

  @Test
  public void grizzlyWorkThreadPoolUsedWhenNoWorkManagerForReadIOEvent() throws IOException {
    new TestFlowWorkManagerIOStrategy(null).executeIoEvent(connection, IOEvent.READ);
    verify(grizzlyWorkerThreadPool, times(1)).execute(any(Runnable.class));
  }

  @Test
  public void selectorUsedForConnectIOEvent() throws IOException {
    new TestFlowWorkManagerIOStrategy(flowWorkManager).executeIoEvent(connection, IOEvent.CLIENT_CONNECTED);
    verify(flowWorkManager, never()).execute(any(Runnable.class));
    verify(grizzlyWorkerThreadPool, never()).execute(any(Runnable.class));
  }

  @Test
  public void selectorUsedWhenNoWorkManagerForReadEvent() throws IOException {
    new TestFlowWorkManagerIOStrategy(null).executeIoEvent(connection, IOEvent.CLIENT_CONNECTED);
    verify(flowWorkManager, never()).execute(any(Runnable.class));
    verify(grizzlyWorkerThreadPool, never()).execute(any(Runnable.class));
  }


  class TestFlowWorkManagerIOStrategy extends FlowWorkManagerIOStrategy {

    private WorkManager workManager;

    TestFlowWorkManagerIOStrategy(WorkManager workManager) {
      this.workManager = workManager;
    }

    @Override
    protected WorkManager getWorkManager(Connection connection) throws MuleException {
      return workManager;
    }
  }

}
