/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import static java.util.Collections.unmodifiableList;

import org.mule.runtime.http.api.domain.message.request.HttpRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Request matcher for HTTP methods.
 */
class DefaultMethodRequestMatcher implements MethodRequestMatcher {

  private final Collection<String> methods;

  /**
   * The list of methods accepted by this matcher
   *
   * @param methods http request method allowed.
   */
  DefaultMethodRequestMatcher(final Collection<String> methods) {
    this.methods = methods;
  }

  /**
   * Determines if there's an intersection between the allowed methods by two request matcher
   *
   * @param matcher request matcher to test against
   * @return true at least there's one http method that both request matcher accepts, false otherwise.
   */
  @Override
  public boolean intersectsWith(final MethodRequestMatcher matcher) {
    checkArgument(matcher != null, "matcher cannot be null");
    for (String method : methods) {
      if (matcher.getMethods().contains(method)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean matches(final HttpRequest httpRequest) {
    return this.methods.contains(httpRequest.getMethod().toUpperCase());
  }

  @Override
  public String toString() {
    return "MethodRequestMatcher{" + "methods=" + Arrays.toString(this.methods.toArray()) + '}';
  }

  @Override
  public List<String> getMethods() {
    return unmodifiableList(new ArrayList<>(methods));
  }

  @Override
  public int hashCode() {
    return 31 * methods.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof DefaultMethodRequestMatcher && Objects.equals(methods, ((DefaultMethodRequestMatcher) obj).methods);
  }
}
