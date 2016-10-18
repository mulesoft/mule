/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

/**
 * Extracts argument values from an {@link ExecutionContext} and exposes them as an array
 *
 * @since 3.7.0
 */
interface ArgumentResolverDelegate {

  /**
   * Returns an object array with the argument values of the given {@code executionContext}
   *
   * @param executionContext the {@link ExecutionContext context} of an {@link ComponentModel} being currently executed
   * @param parameterTypes
   * @return an object array
   */
  Object[] resolve(ExecutionContext executionContext, Class<?>[] parameterTypes);
}
