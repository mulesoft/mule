/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener.matcher;

import org.mule.service.http.api.domain.request.HttpRequest;
import org.mule.service.http.api.server.MethodRequestMatcher;

/**
 * Accepts all the http methods
 */
public class AcceptsAllMethodsRequestMatcher extends DefaultMethodRequestMatcher {

  private static AcceptsAllMethodsRequestMatcher instance = new AcceptsAllMethodsRequestMatcher();

  private AcceptsAllMethodsRequestMatcher() {}

  public boolean matches(HttpRequest httpRequest) {
    return true;
  }

  public static AcceptsAllMethodsRequestMatcher instance() {
    return instance;
  }

  @Override
  public boolean intersectsWith(MethodRequestMatcher methodRequestMatcher) {
    return methodRequestMatcher instanceof AcceptsAllMethodsRequestMatcher;
  }
}
