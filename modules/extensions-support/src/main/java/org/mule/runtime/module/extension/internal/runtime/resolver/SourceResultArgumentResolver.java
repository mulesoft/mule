/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.extension.api.runtime.source.SourceResult.invocationError;
import static org.mule.runtime.extension.api.runtime.source.SourceResult.responseError;
import static org.mule.runtime.extension.api.runtime.source.SourceResult.success;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.extension.api.runtime.source.SourceResult;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.util.Set;

/**
 * {@link ArgumentResolver} implementation which create instances of {@link SourceResult}
 *
 * @since 4.0
 */
public class SourceResultArgumentResolver implements ArgumentResolver<SourceResult> {

  private ArgumentResolver<Error> errorArgumentResolver;
  private ArgumentResolver<SourceCallbackContext> callbackContextArgumentResolver;

  private static final Set<String> GENERATE_ERRORS = of(SOURCE_RESPONSE_GENERATE,
                                                        SOURCE_ERROR_RESPONSE_GENERATE)
                                                            .map(ComponentIdentifier::getName)
                                                            .collect(toSet());

  public SourceResultArgumentResolver(ArgumentResolver<Error> errorArgumentResolver,
                                      ArgumentResolver<SourceCallbackContext> callbackContextArgumentResolver) {
    this.errorArgumentResolver = errorArgumentResolver;
    this.callbackContextArgumentResolver = callbackContextArgumentResolver;
  }

  @Override
  public SourceResult resolve(ExecutionContext executionContext) {
    Error error = errorArgumentResolver.resolve(executionContext);
    SourceCallbackContext callbackContext = callbackContextArgumentResolver.resolve(executionContext);

    if (error == null) {
      return success(callbackContext);
    } else {
      String errorIdentifier = error.getErrorType().getIdentifier();
      return isErrorGeneratingErrorResponse(errorIdentifier)
          ? invocationError(error, callbackContext)
          : responseError(error, callbackContext);
    }
  }

  private boolean isErrorGeneratingErrorResponse(String identifier) {
    return GENERATE_ERRORS.contains(identifier);
  }
}
