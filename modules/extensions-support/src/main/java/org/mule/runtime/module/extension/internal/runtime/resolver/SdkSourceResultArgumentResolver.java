/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.sdk.api.runtime.source.SourceResult.invocationError;
import static org.mule.sdk.api.runtime.source.SourceResult.responseError;
import static org.mule.sdk.api.runtime.source.SourceResult.success;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;
import org.mule.sdk.api.runtime.source.SourceResult;


/**
 * {@link ArgumentResolver} implementation which create instances of {@link SourceResult}
 *
 * @since 4.4.0
 */
public class SdkSourceResultArgumentResolver extends AbstractSourceResultArgumentResolver<SourceResult> {

  private ArgumentResolver<Error> errorArgumentResolver;
  private ArgumentResolver<SourceCallbackContext> callbackContextArgumentResolver;

  public SdkSourceResultArgumentResolver(ArgumentResolver<Error> errorArgumentResolver,
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
