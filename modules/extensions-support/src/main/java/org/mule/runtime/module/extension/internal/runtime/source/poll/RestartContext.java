/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source.poll;

import org.mule.runtime.api.scheduler.Scheduler;

/**
 * Context needed to perform the restart of a source
 *
 * @since 4.2.3 4.3.1 4.4.0
 */
public class RestartContext {

  private Scheduler executor;
  private DelegateRunnable delegateRunnable;

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
