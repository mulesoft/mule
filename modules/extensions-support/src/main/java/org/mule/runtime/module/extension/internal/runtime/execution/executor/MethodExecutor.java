/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.executor;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

/**
 * Executes an underlying {@link MethodExecutor} based on parameters taken from an {@link ExecutionContext}
 *
 * @since 4.3.0
 */
public interface MethodExecutor {

  /**
   * Executes the method
   *
   * @param executionContext an {@link ExecutionContext}
   * @return the method's return value
   * @throws Exception if the method does
   */
  Object execute(ExecutionContext executionContext) throws Exception;
}
