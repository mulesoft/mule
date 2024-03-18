/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;

import java.util.Collection;
import java.util.List;

/**
 * {@link RequestMatcher} that specifically matches against an {@link HttpRequest} method.
 *
 * @since 4.0
 */
@NoImplement
public interface MethodRequestMatcher extends RequestMatcher {

  /**
   * @return a {@link MethodRequestMatcher} that will match any {@link HttpRequest}
   * @since 4.1.5
   */
  static MethodRequestMatcher acceptAll() {
    return AcceptsAllMethodsRequestMatcher.instance();
  }

  /**
   * @return a fresh {@link MethodRequestMatcherBuilder}
   * @since 4.1.5
   */
  static MethodRequestMatcherBuilder builder() {
    return new MethodRequestMatcherBuilder();
  }

  /**
   * Creates a {@link MethodRequestMatcherBuilder} already set up with a collection of methods
   *
   * @param methods a collection of HTTP methods to start with
   * @return a fresh {@link MethodRequestMatcherBuilder}
   * @since 4.1.5
   */
  static MethodRequestMatcherBuilder builder(Collection<String> methods) {
    return new MethodRequestMatcherBuilder(methods);
  }

  /**
   * @param matcher another {@link MethodRequestMatcher}.
   * @return true if this and {@code matcher} have matching methods in common, false otherwise.
   */
  boolean intersectsWith(MethodRequestMatcher matcher);

  /**
   * @return the list of methods to match.
   */
  List<String> getMethods();

  /**
   * @return whether this matcher accepts all HTTP requests
   * @since 4.1.5
   */
  default boolean acceptsAll() {
    return false;
  }

}
