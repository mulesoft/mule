/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.server.grizzly;

import org.mule.service.http.api.server.ServerAddress;
import org.mule.services.http.impl.service.server.ServerAddressMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * {@link ExecutorProvider} implementation that retrieves an
 * {@link Executor} for a {@link ServerAddress}.
 */
public class WorkManagerSourceExecutorProvider implements ExecutorProvider {

  private ServerAddressMap<Supplier<ExecutorService>> executorPerServerAddress =
      new ServerAddressMap<>(new ConcurrentHashMap<ServerAddress, Supplier<ExecutorService>>());

  /**
   * Adds an {@link Executor} to be used when a request is made to a
   * {@link ServerAddress}
   *
   * @param serverAddress address to which the executor should be applied to
   * @param workManagerSource the executor to use when a request is done to the server address
   */
  public void addExecutor(final ServerAddress serverAddress, final Supplier<ExecutorService> workManagerSource) {
    executorPerServerAddress.put(serverAddress, workManagerSource);
  }

  public void removeExecutor(ServerAddress serverAddress) {
    executorPerServerAddress.remove(serverAddress);
  }

  @Override
  public Executor getExecutor(ServerAddress serverAddress) {
    return executorPerServerAddress.get(serverAddress).get();
  }
}
