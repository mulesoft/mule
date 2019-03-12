/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import reactor.core.publisher.Mono;

/**
 * Executes operations while coordinating the several moving parts that are affected by the execution process, so that such pieces
 * can remain decoupled.
 * <p>
 * This mediator will coordinate {@link CompletableComponentExecutor operation executors}, {@link Interceptor interceptors}, configuration
 * expiration, statistics, etc.
 * <p>
 * This mediator supports reactive streams and hence returns the operation result in the form of a {@link Mono}.
 *
 * @since 4.0
 */
public interface ExecutionMediator<M extends ComponentModel> {

  /**
   * Coordinates the execution of the {@code executor} using the given {@code context}
   *
   * @param executor an {@link CompletableComponentExecutor}
   * @param context an {@link ExecutionContextAdapter}
   * @param callback
   */
  void execute(CompletableComponentExecutor<M> executor,
               ExecutionContextAdapter<M> context,
               ExecutorCallback callback);
}
