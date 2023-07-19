/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.extension.api.runtime.source.SourceResult.invocationError;
import static org.mule.runtime.extension.api.runtime.source.SourceResult.responseError;
import static org.mule.runtime.extension.api.runtime.source.SourceResult.success;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.extension.api.runtime.source.SourceResult;

/**
 * {@link ArgumentResolver} implementation which create instances of {@link SourceResult}
 *
 * @since 4.0
 */
public class SourceResultArgumentResolver extends AbstractSourceResultArgumentResolver<SourceResult> {

  private final ArgumentResolver<Error> errorArgumentResolver;
  private final ArgumentResolver<SourceCallbackContext> callbackContextArgumentResolver;

  public SourceResultArgumentResolver(ArgumentResolver<Error> errorArgumentResolver,
                                      ArgumentResolver<SourceCallbackContext> callbackContextArgumentResolver) {
    super(errorArgumentResolver);
    this.errorArgumentResolver = errorArgumentResolver;
    this.callbackContextArgumentResolver = callbackContextArgumentResolver;
  }

  @Override
  protected SourceResult resolveSuccess(ExecutionContext executionContext) {
    return success(callbackContextArgumentResolver.resolve(executionContext));
  }

  @Override
  protected SourceResult resolveInvocationError(ExecutionContext executionContext) {
    return invocationError(errorArgumentResolver.resolve(executionContext),
                           callbackContextArgumentResolver.resolve(executionContext));
  }

  @Override
  protected SourceResult resolveResponseError(ExecutionContext executionContext) {
    return responseError(errorArgumentResolver.resolve(executionContext),
                         callbackContextArgumentResolver.resolve(executionContext));
  }
}
