/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.server.grizzly;

import static java.util.concurrent.Executors.newCachedThreadPool;
import org.mule.runtime.core.util.concurrent.NamedThreadFactory;

import java.util.concurrent.ExecutorService;

import org.glassfish.grizzly.utils.DelayedExecutor;

/**
 * Object containing all idle check executors, making it easy to dispose them once a server goes down.
 */
public class IdleExecutor {

  private static final String IDLE_TIMEOUT_THREADS_PREFIX_NAME = ".HttpIdleConnectionCloser";

  private ExecutorService idleTimeoutExecutorService;
  private DelayedExecutor idleTimeoutDelayedExecutor;

  public IdleExecutor(String prefix) {
    //TODO - MULE-10545: Change http selectors to use a connector pool from the service
    this.idleTimeoutExecutorService = newCachedThreadPool(new NamedThreadFactory(prefix + IDLE_TIMEOUT_THREADS_PREFIX_NAME));
    this.idleTimeoutDelayedExecutor = new DelayedExecutor(idleTimeoutExecutorService);
  }

  public void start() {
    idleTimeoutDelayedExecutor.start();
  }

  public DelayedExecutor getIdleTimeoutDelayedExecutor() {
    return idleTimeoutDelayedExecutor;
  }

  public void dispose() {
    idleTimeoutDelayedExecutor.destroy();
    idleTimeoutExecutorService.shutdown();
  }

}
