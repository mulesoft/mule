/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client;

import org.mule.api.annotation.Experimental;
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

  /**
   * Allows to retrieve a previously created {@link HttpClient}, if used from the same context. Keep in mind lifecycle changes to
   * the retrieved instance won't take effect since only the owner of the server can modify it's status.
   *
   * @param name the name the desired {@link HttpClient} was given when created (see {@link HttpClientConfiguration#getName()})
   * @return the server found
   * @throws ClientNotFoundException when the desired server was not found
   * @since 4.1.5 as experimental.
   */
  @Experimental
  HttpClient lookup(String name) throws ClientNotFoundException;
}
