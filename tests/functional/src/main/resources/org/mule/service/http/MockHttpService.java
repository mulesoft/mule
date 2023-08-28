/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.service.http;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;

import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientFactory;
import org.mule.runtime.http.api.server.HttpServer;
import org.mule.runtime.http.api.server.HttpServerFactory;
import org.mule.runtime.http.api.utils.RequestMatcherRegistry;

public class MockHttpService implements HttpService {

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  public HttpServerFactory getServerFactory() {
    throw new UnsupportedOperationException();
  }

  public HttpClientFactory getClientFactory() {
    throw new UnsupportedOperationException();
  }

  public RequestMatcherRegistry.RequestMatcherRegistryBuilder getRequestMatcherRegistryBuilder() {
    throw new UnsupportedOperationException();
  }

}
