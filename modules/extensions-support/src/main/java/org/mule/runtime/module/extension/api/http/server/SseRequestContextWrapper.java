/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http.server;

import org.mule.runtime.http.api.sse.server.SseRequestContext;
import org.mule.sdk.api.http.domain.message.request.HttpRequestContext;
import org.mule.sdk.api.http.sse.server.SseResponseCustomizer;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SseRequestContextWrapper implements org.mule.sdk.api.http.sse.server.SseRequestContext {

  private final SseRequestContext ctx;

  public SseRequestContextWrapper(SseRequestContext ctx) {
    this.ctx = ctx;
  }

  @Override
  public void setClientId(String overrideId) {
    ctx.setClientId(overrideId);
  }

  @Override
  public CompletableFuture<Void> reject(int statusCode, String reasonPhrase) {
    return ctx.reject(statusCode, reasonPhrase);
  }

  @Override
  public void customizeResponse(Consumer<SseResponseCustomizer> responseCustomizer) {
    ctx.customizeResponse(customizer -> responseCustomizer.accept(new SseResponseCustomizerWrapper(customizer)));
  }

  @Override
  public HttpRequestContext getRequestContext() {
    return new HttpRequestContextWrapper(ctx.getRequestContext());
  }
}
