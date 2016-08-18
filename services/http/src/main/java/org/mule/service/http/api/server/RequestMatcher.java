/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.server;



import org.mule.service.http.api.domain.request.HttpRequest;

/**
 * A request matcher represents a condition that is fulfilled or not by an http request
 */
public interface RequestMatcher {

  /**
   * @param httpRequest request to evaluate against
   *
   * @return true if the request matches the expected condition, false otherwise.
   */
  boolean matches(HttpRequest httpRequest);
}
