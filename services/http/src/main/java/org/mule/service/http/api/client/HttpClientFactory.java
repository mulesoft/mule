/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.client;

/**
 * Factory object for {@link HttpClient} mule-to-httpLibrary adapters.
 */
public interface HttpClientFactory {

  /**
   * @param configuration the configuration to use for the underlying http library.
   * @return a newly built {@link HttpClient}
   */
  HttpClient create(HttpClientConfiguration configuration);

}
