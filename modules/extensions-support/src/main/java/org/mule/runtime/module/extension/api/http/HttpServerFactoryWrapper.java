/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http;

import org.mule.runtime.http.api.server.HttpServerConfiguration;
import org.mule.runtime.http.api.server.ServerCreationException;
import org.mule.sdk.api.http.HttpServer;
import org.mule.sdk.api.http.HttpServerFactory;

public class HttpServerFactoryWrapper implements HttpServerFactory<HttpServerConfiguration, ServerCreationException> {

  private final org.mule.runtime.http.api.server.HttpServerFactory delegate;

  public HttpServerFactoryWrapper(org.mule.runtime.http.api.server.HttpServerFactory delegate) {
    this.delegate = delegate;
  }

  @Override
  public HttpServer create(HttpServerConfiguration configuration) throws ServerCreationException {
    return new HttpServerWrapper(delegate.create(configuration));
  }
}
