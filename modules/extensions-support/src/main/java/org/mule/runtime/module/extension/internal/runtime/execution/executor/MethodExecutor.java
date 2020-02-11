/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
