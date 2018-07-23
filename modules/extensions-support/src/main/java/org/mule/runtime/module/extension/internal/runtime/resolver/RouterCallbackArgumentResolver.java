/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties.COMPLETION_CALLBACK_CONTEXT_PARAM;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.util.function.Supplier;

/**
 * {@link ArgumentResolver} which returns the {@link org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties#COMPLETION_CALLBACK_CONTEXT_PARAM}
 * context variable.
 * <p>
 * Notice that this resolver only works if the {@link ExecutionContext} is a {@link ExecutionContextAdapter}
 *
 * @since 4.0
 */
public final class RouterCallbackArgumentResolver implements ArgumentResolver<RouterCompletionCallback> {

  @Override
  public Supplier<RouterCompletionCallback> resolve(ExecutionContext executionContext) {
    return () -> {
      CompletionCallback completionCallback = (CompletionCallback) ((ExecutionContextAdapter) executionContext)
          .getVariable(COMPLETION_CALLBACK_CONTEXT_PARAM);

      return new RouterCompletionCallback() {

        @Override
        public void success(Result result) {
          completionCallback.success(result);
        }

        @Override
        public void error(Throwable e) {
          completionCallback.error(e);
        }
      };
    };
  }
}
