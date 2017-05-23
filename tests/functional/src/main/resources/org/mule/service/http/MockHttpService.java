/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;

import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientFactory;
import org.mule.runtime.http.api.server.HttpServer;
import org.mule.runtime.http.api.server.HttpServerFactory;

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

}
