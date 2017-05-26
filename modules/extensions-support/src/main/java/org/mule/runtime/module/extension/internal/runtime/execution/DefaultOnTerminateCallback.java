/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.extension.api.OnTerminateCallback;

import java.util.function.Consumer;

/**
 * mule-alltogether
 *
 * @author Esteban Wasinger (http://github.com/estebanwasinger)
 */
public class DefaultOnTerminateCallback implements OnTerminateCallback {


  private static final Consumer NULL_CONSUMER = e -> {
  };
  private OnTerminateCallback callback;

  public DefaultOnTerminateCallback(OnTerminateCallback callback) {

    this.callback = callback;
  }

  public static DefaultOnTerminateCallback success() {
    OnTerminateCallback successCallback = new OnTerminateCallback() {

      @Override
      public void execute(Consumer<Void> onSuccess, Consumer<Error> onParameterResolutionError, Consumer<Error> onResponseError) {
        onSuccess.accept(null);
      }

      @Override
      public void execute(Consumer<Error> onParameterResolutionError, Consumer<Error> onResponseError) {
        execute(NULL_CONSUMER, NULL_CONSUMER, NULL_CONSUMER);
      }
    };
    return new DefaultOnTerminateCallback(successCallback);
  }

  public static DefaultOnTerminateCallback bodyError(Error error) {
    OnTerminateCallback successCallback = new OnTerminateCallback() {

      @Override
      public void execute(Consumer<Void> onSuccess, Consumer<Error> onParameterResolutionError, Consumer<Error> onResponseError) {
        onResponseError.accept(error);
      }

      @Override
      public void execute(Consumer<Error> onParameterResolutionError, Consumer<Error> onResponseError) {
        execute(NULL_CONSUMER, NULL_CONSUMER, onResponseError);
      }
    };
    return new DefaultOnTerminateCallback(successCallback);
  }

  public static DefaultOnTerminateCallback parametersError(Error error) {
    OnTerminateCallback successCallback = new OnTerminateCallback() {

      @Override
      public void execute(Consumer<Void> onSuccess, Consumer<Error> onParameterResolutionError, Consumer<Error> onResponseError) {
        onParameterResolutionError.accept(error);
      }

      @Override
      public void execute(Consumer<Error> onParameterResolutionError, Consumer<Error> onResponseError) {
        execute(NULL_CONSUMER, onParameterResolutionError, NULL_CONSUMER);
      }
    };
    return new DefaultOnTerminateCallback(successCallback);
  }


  @Override
  public void execute(Consumer<Void> onSuccess, Consumer<Error> onParameterResolutionError, Consumer<Error> onResponseError) {
    callback.execute(onSuccess, onParameterResolutionError, onResponseError);
  }

  @Override
  public void execute(Consumer<Error> onParameterResolutionError, Consumer<Error> onResponseError) {
    callback.execute(NULL_CONSUMER, onParameterResolutionError, onResponseError);
  }
}
