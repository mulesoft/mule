/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener.grizzly;

import org.mule.runtime.module.http.internal.listener.DefaultServerAddress;
import org.mule.service.http.api.server.ServerAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.IOEvent;
import org.glassfish.grizzly.IOEventLifeCycleListener;
import org.glassfish.grizzly.strategies.AbstractIOStrategy;

/**
 * Grizzly IO Strategy that will handle each work to an specific {@link java.util.concurrent.Executor} based on the
 * {@link ServerAddress} of a {@link org.glassfish.grizzly.Connection}.
 * <p/>
 * There's logic from {@link org.glassfish.grizzly.strategies.WorkerThreadIOStrategy} that need to be reused but unfortunately
 * that class cannot be override.
 */
public class ExecutorPerServerAddressIOStrategy extends AbstractIOStrategy {

  private final static EnumSet<IOEvent> WORKER_THREAD_EVENT_SET = EnumSet.of(IOEvent.READ, IOEvent.CLOSED);

  private static final Logger logger = Grizzly.logger(ExecutorPerServerAddressIOStrategy.class);
  private final ExecutorProvider executorProvider;

  public ExecutorPerServerAddressIOStrategy(final ExecutorProvider executorProvider) {
    this.executorProvider = executorProvider;
  }

  @Override
  public boolean executeIoEvent(final Connection connection, final IOEvent ioEvent, final boolean isIoEventEnabled)
      throws IOException {

    final boolean isReadOrWriteEvent = isReadWrite(ioEvent);

    final IOEventLifeCycleListener listener;
    if (isReadOrWriteEvent) {
      if (isIoEventEnabled) {
        connection.disableIOEvent(ioEvent);
      }

      listener = ENABLE_INTEREST_LIFECYCLE_LISTENER;
    } else {
      listener = null;
    }

    final Executor threadPool = getThreadPoolFor(connection, ioEvent);
    if (threadPool != null) {
      threadPool.execute(new WorkerThreadRunnable(connection, ioEvent, listener));
    } else {
      run0(connection, ioEvent, listener);
    }

    return true;
  }

  @Override
  public Executor getThreadPoolFor(Connection connection, IOEvent ioEvent) {
    if (WORKER_THREAD_EVENT_SET.contains(ioEvent)) {
      final String ip = ((InetSocketAddress) connection.getLocalAddress()).getAddress().getHostAddress();
      final int port = ((InetSocketAddress) connection.getLocalAddress()).getPort();
      return executorProvider.getExecutor(new DefaultServerAddress(ip, port));
    } else {
      // Run other types of IOEvent in selector thread.
      return null;
    }
  }

  private static void run0(final Connection connection, final IOEvent ioEvent, final IOEventLifeCycleListener lifeCycleListener) {

    fireIOEvent(connection, ioEvent, lifeCycleListener, logger);

  }

  private static final class WorkerThreadRunnable implements Runnable {

    final Connection connection;
    final IOEvent ioEvent;
    final IOEventLifeCycleListener lifeCycleListener;

    private WorkerThreadRunnable(final Connection connection, final IOEvent ioEvent,
                                 final IOEventLifeCycleListener lifeCycleListener) {
      this.connection = connection;
      this.ioEvent = ioEvent;
      this.lifeCycleListener = lifeCycleListener;
    }

    @Override
    public void run() {
      run0(connection, ioEvent, lifeCycleListener);
    }
  }

}
