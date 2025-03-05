/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

/**
 * Factory object for {@link HttpClient}.
 *
 * @since 4.0
 */
@NoImplement
public interface HttpClientFactory
    extends org.mule.sdk.api.http.HttpClientFactory<HttpClientConfiguration, HttpRequest, HttpRequestOptions, HttpResponse> {

  /**
   * @param configuration the {@link HttpClientConfiguration} specifying the desired client.
   * @return a newly built {@link HttpClient} based on the {@code configuration}.
   */
  HttpClient create(HttpClientConfiguration configuration);
}
