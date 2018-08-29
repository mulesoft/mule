/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.http;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.ClientNotFoundException;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.client.HttpClientFactory;
import org.mule.runtime.http.api.client.auth.HttpAuthentication;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.entity.EmptyHttpEntity;
import org.mule.runtime.http.api.domain.entity.HttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.runtime.http.api.server.HttpServerFactory;
import org.mule.runtime.http.api.utils.RequestMatcherRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * This HTTP service fake implementation validates resource access via it's clients send method. It allows testing which resources
 * will be accessible to a service classloader.
 */
public class ResourceHttpService implements HttpService {

  @Override
  public HttpServerFactory getServerFactory() {
    return null;
  }

  @Override
  public HttpClientFactory getClientFactory() {
    return new HttpClientFactory() {

      @Override
      public HttpClient create(HttpClientConfiguration configuration) {
        return new HttpClient() {

          /**
           * Loads MANIFEST resources and gets their Bundle-Description.
           *
           * @param request the {@link HttpRequest} to send (the entities bytes will be treated as the resource name)
           * @param responseTimeout the timeout (ignored)
           * @param followRedirects whether to follow redirect responses (ignored)
           * @param authentication the authentication to use (ignored)
           * @return the Manifest Bundle-Descriptor or an empty String if not found.
           * @throws IOException
           * @throws TimeoutException
           */
          @Override
          public HttpResponse send(HttpRequest request, int responseTimeout, boolean followRedirects,
                                   HttpAuthentication authentication)
              throws IOException, TimeoutException {
            String resource = new String(request.getEntity().getBytes());
            InputStream resourceStream = this.getClass().getClassLoader().getResourceAsStream(resource);
            String body = null;
            if (resourceStream != null) {
              Properties properties = new Properties();
              try {
                properties.load(resourceStream);
              } catch (IOException e) {
                throw new MuleRuntimeException(e);
              }
              body = properties.getProperty("Bundle-Description");
            }
            return HttpResponse.builder()
                .entity(resolveEntity(body))
                .build();
          }

          @Override
          public CompletableFuture<HttpResponse> sendAsync(HttpRequest request, int responseTimeout, boolean followRedirects,
                                                           HttpAuthentication authentication) {
            return null;
          }

          @Override
          public void start() {

          }

          @Override
          public void stop() {

          }
        };
      }

      @Override
      public HttpClient lookup(String name) throws ClientNotFoundException {
        throw new UnsupportedOperationException("not implemented for this test");
      }
    };
  }

  @Override
  public RequestMatcherRegistry.RequestMatcherRegistryBuilder getRequestMatcherRegistryBuilder() {
    return null;
  }

  private HttpEntity resolveEntity(String bundleDescription) {
    return bundleDescription == null ? new EmptyHttpEntity() : new ByteArrayHttpEntity(bundleDescription.getBytes());
  }

  @Override
  public String getName() {
    return "Resource HTTP Service";
  }
}
