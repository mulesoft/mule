/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.source.poll;

import org.mule.runtime.api.scheduler.Scheduler;

/**
 * Context needed to perform the restart of a source
 *
 * @since 4.2.3 4.3.1 4.4.0
 */
public class RestartContext {

  private final Scheduler executor;
  private final DelegateRunnable delegateRunnable;

  public RestartContext(Scheduler executor, DelegateRunnable delegateRunnable) {
    this.executor = executor;
    this.delegateRunnable = delegateRunnable;
  }

  public Scheduler getExecutor() {
    return executor;
  }

  public DelegateRunnable getDelegateRunnable() {
    return delegateRunnable;
  }
}
