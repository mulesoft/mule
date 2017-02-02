/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.client.HttpClientConfiguration;
import org.mule.service.http.api.client.HttpRequestAuthentication;
import org.mule.service.http.api.client.async.ResponseHandler;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

/**
 * Manages {@link HttpClient HttpClients} across multiple configurations based on their name, meaning two configurations spawning
 * from the same prototype will receive the same {@link HttpClient}.
 *
 * @since 4.0
 */
public class HttpRequesterConnectionManager implements Disposable {

  @Inject
  private HttpService httpService;

  private Map<String, HttpClient> clients = new HashMap<>();

  public HttpRequesterConnectionManager() {}

  public HttpRequesterConnectionManager(HttpService httpService) {
    this.httpService = httpService;
  }

  /**
   * Searches for an already existing {@link HttpClient} associated with the desired configuration name.
   *
   * @param configName the name of the client to look for
   * @return an {@link Optional} with an {@link HttpClient} if found or an empty one otherwise
   */
  public Optional<HttpClient> lookup(String configName) {
    return ofNullable(clients.get(configName));
  }

  /**
   * Creates an {@link HttpClient} associated with the given configuration name. If there's already one, this operation will fail
   * so {@link #lookup(String)} should be used first.
   *
   * @param configName
   * @param clientConfiguration
   * @return
   */
  public synchronized HttpClient create(String configName, HttpClientConfiguration clientConfiguration) {
    checkArgument(!clients.containsKey(configName), format("There's an HttpClient available for %s already.", configName));
    ShareableHttpClient client = new ShareableHttpClient(httpService.getClientFactory().create(clientConfiguration));
    clients.put(configName, client);
    return client;
  }

  @Override
  public void dispose() {
    clients.clear();
  }

  /**
   * Proxy implementation of an {@link HttpClient} that allows being shared by only configuring the client when first required and
   * only disabling it when last required.
   */
  private class ShareableHttpClient implements HttpClient {

    private HttpClient delegate;
    private AtomicInteger usageCount = new AtomicInteger(0);

    ShareableHttpClient(HttpClient client) {
      delegate = client;
    }

    @Override
    public void start() {
      if (usageCount.incrementAndGet() == 1) {
        delegate.start();
      }
    }

    @Override
    public void stop() {
      if (usageCount.decrementAndGet() == 0) {
        delegate.stop();
      }
    }

    @Override
    public HttpResponse send(HttpRequest request, int responseTimeout, boolean followRedirects,
                             HttpRequestAuthentication authentication)
        throws IOException, TimeoutException {
      return delegate.send(request, responseTimeout, followRedirects, authentication);
    }

    @Override
    public void send(HttpRequest request, int responseTimeout, boolean followRedirects, HttpRequestAuthentication authentication,
                     ResponseHandler handler) {
      delegate.send(request, responseTimeout, followRedirects, authentication, handler);
    }
  }
}
