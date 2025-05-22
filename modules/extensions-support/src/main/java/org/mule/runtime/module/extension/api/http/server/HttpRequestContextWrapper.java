/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http.server;

import org.mule.runtime.module.extension.api.http.message.muletosdk.HttpRequestWrapper;
import org.mule.sdk.api.http.domain.message.request.ClientConnection;
import org.mule.sdk.api.http.domain.message.request.HttpRequest;
import org.mule.sdk.api.http.domain.message.request.HttpRequestContext;
import org.mule.sdk.api.http.domain.message.request.ServerConnection;

public class HttpRequestContextWrapper implements HttpRequestContext {

  private final org.mule.runtime.http.api.domain.request.HttpRequestContext muleRequestContext;

  public HttpRequestContextWrapper(org.mule.runtime.http.api.domain.request.HttpRequestContext requestContext) {
    this.muleRequestContext = requestContext;
  }

  @Override
  public String getScheme() {
    return muleRequestContext.getScheme();
  }

  @Override
  public HttpRequest getRequest() {
    return new HttpRequestWrapper(muleRequestContext.getRequest());
  }

  @Override
  public ServerConnection getServerConnection() {
    return new ServerConnectionWrapper(muleRequestContext.getServerConnection());
  }

  @Override
  public ClientConnection getClientConnection() {
    return new ClientConnectionWrapper(muleRequestContext.getClientConnection());
  }
}
