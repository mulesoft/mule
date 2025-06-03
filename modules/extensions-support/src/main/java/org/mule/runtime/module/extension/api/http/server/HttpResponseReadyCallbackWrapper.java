/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http.server;

import org.mule.runtime.module.extension.api.http.message.sdktomule.HttpResponseWrapper;
import org.mule.sdk.api.http.domain.message.response.HttpResponse;
import org.mule.sdk.api.http.server.async.HttpResponseReadyCallback;
import org.mule.sdk.api.http.server.async.ResponseStatusCallback;
import org.mule.sdk.api.http.sse.server.SseClient;
import org.mule.sdk.api.http.sse.server.SseClientConfig;

import java.util.function.Consumer;

public class HttpResponseReadyCallbackWrapper implements HttpResponseReadyCallback {

  private final org.mule.runtime.http.api.server.async.HttpResponseReadyCallback muleResponseCallback;

  public HttpResponseReadyCallbackWrapper(org.mule.runtime.http.api.server.async.HttpResponseReadyCallback muleResponseCallback) {
    this.muleResponseCallback = muleResponseCallback;
  }

  @Override
  public void responseReady(HttpResponse response, ResponseStatusCallback responseStatusCallback) {
    muleResponseCallback.responseReady(new HttpResponseWrapper(response),
                                       new ResponseStatusCallbackWrapper(responseStatusCallback));
  }

  @Override
  public SseClient startSseResponse(Consumer<SseClientConfig> configConsumer) {
    var configurer = new SseClientConfigImpl();
    configConsumer.accept(configurer);
    return new SseClientWrapper(muleResponseCallback.startSseResponse(configurer.build()));
  }
}
