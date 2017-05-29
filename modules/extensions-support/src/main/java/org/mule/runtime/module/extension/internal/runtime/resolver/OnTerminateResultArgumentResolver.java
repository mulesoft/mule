/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.extension.api.runtime.source.OnTerminateResult.parameterError;
import static org.mule.runtime.extension.api.runtime.source.OnTerminateResult.responseError;
import static org.mule.runtime.extension.api.runtime.source.OnTerminateResult.success;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.extension.api.runtime.source.OnTerminateResult;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.util.Set;

/**
 * {@link ArgumentResolver} implementation which create instances of {@link OnTerminateResult}
 *
 * @since 4.0
 */
public class OnTerminateResultArgumentResolver implements ArgumentResolver<OnTerminateResult> {

  private ArgumentResolver<Error> errorArgumentResolver;
  private ArgumentResolver<SourceCallbackContext> callbackContextArgumentResolver;

  private static final Set<String> PARAMETERS_ERRORS = of(SOURCE_RESPONSE_GENERATE,
                                                          SOURCE_ERROR_RESPONSE_GENERATE)
                                                              .map(ComponentIdentifier::getName)
                                                              .collect(toSet());

  public OnTerminateResultArgumentResolver(ArgumentResolver<Error> errorArgumentResolver,
                                           ArgumentResolver<SourceCallbackContext> callbackContextArgumentResolver) {
    this.errorArgumentResolver = errorArgumentResolver;
    this.callbackContextArgumentResolver = callbackContextArgumentResolver;
  }

  @Override
  public OnTerminateResult resolve(ExecutionContext executionContext) {
    Error resolve = errorArgumentResolver.resolve(executionContext);
    SourceCallbackContext callbackContext = callbackContextArgumentResolver.resolve(executionContext);

    if (resolve == null) {
      return success(callbackContext);
    } else {
      String errorIdentifier = resolve.getErrorType().getIdentifier();
      return isErrorGeneratingCallbackParameters(errorIdentifier)
          ? parameterError(resolve, callbackContext)
          : responseError(resolve, callbackContext);
    }
  }

  private boolean isErrorGeneratingCallbackParameters(String identifier) {
    return PARAMETERS_ERRORS.contains(identifier);
  }
}
