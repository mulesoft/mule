/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
