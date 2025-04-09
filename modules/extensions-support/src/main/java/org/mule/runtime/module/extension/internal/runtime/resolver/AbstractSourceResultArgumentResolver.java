/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE_GENERATE;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

import java.util.Set;

/**
 * Abstract {@link ArgumentResolver} which holds the logic to resolve Source Results.
 *
 * @since 4.5.0
 */
public abstract class AbstractSourceResultArgumentResolver<T> implements ArgumentResolver<T> {

  private ArgumentResolver<Error> errorArgumentResolver;

  private static final Set<String> GENERATE_ERRORS = of(SOURCE_RESPONSE_GENERATE,
                                                        SOURCE_ERROR_RESPONSE_GENERATE)
      .map(ComponentIdentifier::getName)
      .collect(toSet());

  public AbstractSourceResultArgumentResolver(ArgumentResolver<Error> errorArgumentResolver) {
    this.errorArgumentResolver = errorArgumentResolver;
  }

  @Override
  public T resolve(ExecutionContext executionContext) {
    Error error = errorArgumentResolver.resolve(executionContext);

    if (error == null) {
      return resolveSuccess(executionContext);
    } else {
      String errorIdentifier = error.getErrorType().getIdentifier();
      return isErrorGeneratingErrorResponse(errorIdentifier)
          ? resolveInvocationError(executionContext)
          : resolveResponseError(executionContext);
    }
  }

  protected abstract T resolveSuccess(ExecutionContext executionContext);

  protected abstract T resolveInvocationError(ExecutionContext executionContext);

  protected abstract T resolveResponseError(ExecutionContext executionContext);

  private boolean isErrorGeneratingErrorResponse(String identifier) {
    return GENERATE_ERRORS.contains(identifier);
  }
}
