/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.server;

import static java.lang.System.identityHashCode;
import static java.util.Collections.singletonList;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;

/**
 * Accepts all the HTTP methods.
 */
class AcceptsAllMethodsRequestMatcher extends DefaultMethodRequestMatcher {

  private static AcceptsAllMethodsRequestMatcher instance = new AcceptsAllMethodsRequestMatcher();

  private AcceptsAllMethodsRequestMatcher() {
    super(singletonList("*"));
  }

  @Override
  public boolean matches(HttpRequest httpRequest) {
    return true;
  }

  static AcceptsAllMethodsRequestMatcher instance() {
    return instance;
  }

  @Override
  public boolean intersectsWith(MethodRequestMatcher matcher) {
    return matcher instanceof AcceptsAllMethodsRequestMatcher;
  }

  @Override
  public int hashCode() {
    return identityHashCode(this);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof AcceptsAllMethodsRequestMatcher;
  }

  @Override
  public boolean acceptsAll() {
    return true;
  }
}
