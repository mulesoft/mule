/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import org.mule.extension.api.runtime.Interceptor;
import org.mule.extension.api.runtime.OperationContext;
import org.mule.extension.api.runtime.OperationExecutor;
import org.mule.extension.api.runtime.RetryRequest;

/**
 * Executes operations while coordinating the several moving parts that are
 * affected by the execution process, so that such pieces can remain decoupled.
 * <p/>
 * This mediator will coordinate {@link OperationExecutor operation executors},
 * {@link Interceptor interceptors}, {@link RetryRequest}, configuration expiration,
 * statistics, etc.
 *
 * @since 4.0
 */
public interface ExecutionMediator
{

    /**
     * Coordinates the execution of the {@code executor} using the given {@code context}
     *
     * @param executor a {@link OperationExecutor}
     * @param context  a {@link OperationContext}
     * @return the operation's result
     * @throws Exception if any exception is encountered
     */
    Object execute(OperationExecutor executor, OperationContext context) throws Exception;
}
