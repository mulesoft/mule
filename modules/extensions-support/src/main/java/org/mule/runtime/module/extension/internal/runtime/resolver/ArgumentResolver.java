/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

/**
 * A component for resolving the value of an operation's argument
 *
 * @param <T> the type of the argument to be resolved
 * @since 3.7.0
 */
public interface ArgumentResolver<T> {

  /**
   * Resolves an argument's value from the given {@code executionContext}
   *
   * @param executionContext an {@link ExecutionContext}
   * @return a value
   */
  T resolve(ExecutionContext executionContext);
}
