/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.consumer.operations;

/**
 * An Operation executed in the context a {@link org.mule.runtime.api.profiling.ProfilingDataConsumer}
 *
 * @param <T> the type for the parameter.
 */
public interface ProfilingExecutionOperation<T> {

  /**
   * Executes the operation.
   *
   * @param parameter the parameter for the operation.
   */
  void execute(T parameter);
}
