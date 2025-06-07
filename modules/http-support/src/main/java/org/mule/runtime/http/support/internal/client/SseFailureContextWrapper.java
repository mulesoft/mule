/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.client;

import org.mule.runtime.http.api.sse.client.SseFailureContext;
import org.mule.runtime.http.support.internal.message.muletosdk.HttpResponseWrapper;
import org.mule.sdk.api.http.domain.message.response.HttpResponse;

public class SseFailureContextWrapper implements org.mule.sdk.api.http.sse.client.SseFailureContext {

  private final SseFailureContext ctx;

  public SseFailureContextWrapper(SseFailureContext ctx) {
    this.ctx = ctx;
  }

  @Override
  public Throwable error() {
    return ctx.error();
  }

  @Override
  public HttpResponse response() {
    return new HttpResponseWrapper(ctx.response());
  }

  @Override
  public void stopRetrying() {
    ctx.stopRetrying();
  }
}
