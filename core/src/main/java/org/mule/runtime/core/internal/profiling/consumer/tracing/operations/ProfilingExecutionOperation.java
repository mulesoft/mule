/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.operations;

/**
 * An operation executed in the context of a {@link org.mule.runtime.api.profiling.ProfilingDataConsumer}
 *
 * @param <T> the type for the parameter of the operation.
 */
public interface ProfilingExecutionOperation<T> {

  /**
   * Executes the operation.
   *
   * @param parameter the parameter for the operation.
   */
  void execute(T parameter);
}
