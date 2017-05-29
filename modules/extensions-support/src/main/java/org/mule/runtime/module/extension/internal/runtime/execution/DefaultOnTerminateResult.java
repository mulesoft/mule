/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.extension.api.OnTerminateResult;

import java.util.function.Consumer;

/**
 * Default {@link OnTerminateResult} implementation.
 *
 * @since 4.0
 */
public final class DefaultOnTerminateResult implements OnTerminateResult {

  private OnTerminateResult callback;

  private DefaultOnTerminateResult(OnTerminateResult callback) {
    this.callback = callback;
  }

  /**
   * @return a new {@link OnTerminateResult} instance for a success scenario which will only execute the {@code onSuccess}
   * scenario.
   */
  public static DefaultOnTerminateResult success() {
    return new DefaultOnTerminateResult((onSuccess, onParameterResolutionError, onResponseError) -> onSuccess.run());
  }

  /**
   * @param error to inject to the {@code onParameterResolutionError} consumer
   * @return a new {@link OnTerminateResult} instance for a parameter resolution error scenario which will only execute
   * the {@code onParameterResolutionError} scenario.
   */
  public static DefaultOnTerminateResult responseError(Error error) {
    return new DefaultOnTerminateResult((onSuccess, onParameterResolutionError, onResponseError) -> onResponseError
        .accept(error));
  }

  /**
   * @param error to inject to the {@code onResponseError} consumer
   * @return a new {@link OnTerminateResult} instance for a response error scenario which will only execute
   * the {@code onResponseError} scenario.
   */
  public static DefaultOnTerminateResult parameterResolutionError(Error error) {
    return new DefaultOnTerminateResult((onSuccess, onParameterResolutionError, onResponseError) -> onParameterResolutionError
        .accept(error));
  }

  @Override
  public void execute(Runnable onSuccess, Consumer<Error> onParameterResolutionError, Consumer<Error> onResponseError) {
    callback.execute(onSuccess, onParameterResolutionError, onResponseError);
  }
}
