/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.client;

import org.mule.api.annotation.NoImplement;

/**
 * Factory object for {@link HttpClient}.
 *
 * @since 4.0
 */
@NoImplement
public interface HttpClientFactory {

  /**
   * @param configuration the {@link HttpClientConfiguration} specifying the desired client.
   * @return a newly built {@link HttpClient} based on the {@code configuration}.
   */
  HttpClient create(HttpClientConfiguration configuration);
}
