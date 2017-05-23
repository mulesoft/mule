/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client;

/**
 * Factory object for {@link HttpClient}.
 *
 * @since 4.0
 */
public interface HttpClientFactory {

  /**
   * @param configuration the {@link HttpClientConfiguration} specifying the desired client.
   * @return a newly built {@link HttpClient} based on the {@code configuration}.
   */
  HttpClient create(HttpClientConfiguration configuration);

}
