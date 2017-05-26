/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.extension.api.OnTerminateInformation;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver;

/**
 * mule-alltogether
 *
 * @author Esteban Wasinger (http://github.com/estebanwasinger)
 */
public class OnTerminateAR implements ArgumentResolver<OnTerminateInformation> {

  private final ArgumentResolver<Error> errorArgumentResolver;
  private final ArgumentResolver<SourceCallbackContext> callbackContextArgumentResolver;

  OnTerminateAR(ArgumentResolver<Error> errorArgumentResolver,
                ArgumentResolver<SourceCallbackContext> callbackContextArgumentResolver) {
    this.errorArgumentResolver = errorArgumentResolver;
    this.callbackContextArgumentResolver = callbackContextArgumentResolver;
  }

  @Override
  public OnTerminateInformation resolve(ExecutionContext executionContext) {
    return new OnTerminateInformation(callbackContextArgumentResolver.resolve(executionContext),
                                      errorArgumentResolver.resolve(executionContext));
  }
}
