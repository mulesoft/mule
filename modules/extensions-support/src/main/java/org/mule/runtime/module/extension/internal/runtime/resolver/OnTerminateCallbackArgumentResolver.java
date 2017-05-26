/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.module.extension.internal.runtime.execution.DefaultOnTerminateCallback.bodyError;
import static org.mule.runtime.module.extension.internal.runtime.execution.DefaultOnTerminateCallback.parametersError;
import static org.mule.runtime.module.extension.internal.runtime.execution.DefaultOnTerminateCallback.success;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.exception.SourceParametersException;
import org.mule.runtime.extension.api.OnTerminateCallback;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

/**
* @since 4.0
 */
public class OnTerminateCallbackArgumentResolver implements ArgumentResolver<OnTerminateCallback> {

  private ArgumentResolver<Error> errorArgumentResolver;

  public OnTerminateCallbackArgumentResolver(ArgumentResolver<Error> errorArgumentResolver) {
    this.errorArgumentResolver = errorArgumentResolver;
  }

  @Override
  public OnTerminateCallback resolve(ExecutionContext executionContext) {
    Error resolve = errorArgumentResolver.resolve(executionContext);
    if (resolve == null) {
      return success();
    } else if (resolve.getCause() instanceof SourceParametersException) {
      return parametersError(resolve);
    } else {
      return bodyError(resolve);
    }
  }
}
