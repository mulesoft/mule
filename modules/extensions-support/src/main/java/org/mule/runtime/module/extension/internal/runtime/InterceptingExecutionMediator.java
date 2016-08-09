/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static java.lang.String.format;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.INTERCEPTING_CALLBACK_PARAM;
import org.mule.runtime.extension.api.runtime.operation.InterceptingCallback;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.module.extension.internal.ExtensionProperties;

/**
 * An {@link ExecutionMediator} for intercepting operations.
 * <p>
 * It works by wrapping another mediator which is expected to return an {@link InterceptingCallback}
 * ({@link IllegalStateException} will be thrown otherwise). Once the callback has been obtained, it is placed as an
 * {@link OperationContextAdapter} variable with key {@link ExtensionProperties#INTERCEPTING_CALLBACK_PARAM} and returns the value
 * of {@link InterceptingCallback#getResult()}.
 * <p>
 * This class is not responsible from actually invoking the intercepted chain nor invoking additional methods on the callback.
 *
 * @since 4.0
 */
public final class InterceptingExecutionMediator implements ExecutionMediator {

  private final ExecutionMediator intercepted;

  /**
   * Creates a new instance
   *
   * @param intercepted a mediator which will actually execute the operation
   */
  public InterceptingExecutionMediator(ExecutionMediator intercepted) {
    this.intercepted = intercepted;
  }

  @Override
  public Object execute(OperationExecutor executor, OperationContextAdapter context) throws Throwable {
    Object resultValue = intercepted.execute(executor, context);
    if (!(resultValue instanceof InterceptingCallback)) {
      throw new IllegalStateException(format("operation '%s' was expected to return a '%s' but a '%s' was found instead",
                                             context.getOperationModel().getName(), InterceptingCallback.class.getSimpleName(),
                                             resultValue));
    }

    context.setVariable(INTERCEPTING_CALLBACK_PARAM, resultValue);

    return ((InterceptingCallback) resultValue).getResult();
  }


}
