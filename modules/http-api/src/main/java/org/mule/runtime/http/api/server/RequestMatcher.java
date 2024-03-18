/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;

/**
 * A request matcher represents a condition that is fulfilled or not by an HTTP request.
 *
 * @since 4.0
 */
@NoImplement
public interface RequestMatcher {

  /**
   * @param httpRequest request to evaluate against
   * @return true if the request matches the expected condition, false otherwise.
   */
  boolean matches(HttpRequest httpRequest);
}
