/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import org.mule.runtime.http.api.server.async.ResponseStatusCallback;

public class ResponseStatusCallbackWrapper implements ResponseStatusCallback {

  private final org.mule.sdk.api.http.server.async.ResponseStatusCallback sdkCallback;

  public ResponseStatusCallbackWrapper(org.mule.sdk.api.http.server.async.ResponseStatusCallback sdkCallback) {
    this.sdkCallback = sdkCallback;
  }

  @Override
  public void responseSendFailure(Throwable throwable) {
    sdkCallback.responseSendFailure(throwable);
  }

  @Override
  public void responseSendSuccessfully() {
    sdkCallback.responseSendSuccessfully();
  }

  @Override
  public void onErrorSendingResponse(Throwable throwable) {
    sdkCallback.onErrorSendingResponse(throwable);
  }
}
