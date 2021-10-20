/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.sdk.api.annotation.execution.OnError;
import org.mule.sdk.api.annotation.execution.OnSuccess;
import org.mule.sdk.api.annotation.execution.OnTerminate;

@Alias("pet-source-with-sdk-api")
public class PetStoreSimpleSourceWithSdkApi extends Source<Void, Void> {

  public static int ON_SUCCESS_CALL_COUNT;
  public static int ON_ERROR_CALL_COUNT;
  public static int ON_TERMINATE_CALL_COUNT;

  @OnSuccess
  public synchronized void onSuccess() {
    ON_SUCCESS_CALL_COUNT++;
  }

  @OnError
  public synchronized void onError() {
    ON_ERROR_CALL_COUNT++;
  }

  @OnTerminate
  public synchronized void onTerminate() {
    ON_TERMINATE_CALL_COUNT++;
  }

  @Override
  public void onStart(SourceCallback<Void, Void> sourceCallback) throws MuleException {
    resetCounters();
    SourceCallbackContext context = sourceCallback.createContext();
    Result result = Result.<String, Object>builder().build();
    sourceCallback.handle(result, context);
  }

  @Override
  public void onStop() {
    resetCounters();
  }

  private synchronized void resetCounters() {
    ON_SUCCESS_CALL_COUNT = ON_ERROR_CALL_COUNT = ON_TERMINATE_CALL_COUNT = 0;
  }
}
