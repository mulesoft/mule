/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import org.mule.runtime.extension.api.runtime.RetryRequest;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;

/**
 * Executes operations while coordinating the several moving parts that are affected by the execution process, so that such pieces
 * can remain decoupled.
 * <p/>
 * This mediator will coordinate {@link OperationExecutor operation executors}, {@link Interceptor interceptors},
 * {@link RetryRequest}, configuration expiration, statistics, etc.
 *
 * @since 4.0
 */
public interface ExecutionMediator {

  /**
   * Coordinates the execution of the {@code executor} using the given {@code context}
   *
   * @param executor an {@link OperationExecutor}
   * @param context an {@link OperationContextAdapter}
   * @return the operation's result
   * @throws Exception if any exception is encountered
   */
  Object execute(OperationExecutor executor, OperationContextAdapter context) throws Throwable;
}
