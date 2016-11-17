/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.listener;

import org.mule.runtime.module.http.internal.listener.async.HttpResponseReadyCallback;

/**
 * Holds temporary state necessary to emit an http response
 *
 * @since 4.0
 */
public class HttpResponseContext {

  private String httpVersion;
  private boolean supportStreaming = true;
  private HttpResponseReadyCallback responseCallback;

  public String getHttpVersion() {
    return httpVersion;
  }

  public void setHttpVersion(String httpVersion) {
    this.httpVersion = httpVersion;
  }

  public boolean isSupportStreaming() {
    return supportStreaming;
  }

  public void setSupportStreaming(boolean supportStreaming) {
    this.supportStreaming = supportStreaming;
  }

  public HttpResponseReadyCallback getResponseCallback() {
    return responseCallback;
  }

  public void setResponseCallback(HttpResponseReadyCallback responseCallback) {
    this.responseCallback = responseCallback;
  }
}
