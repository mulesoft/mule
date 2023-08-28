/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.server;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;

/**
 * {@link RequestMatcher} that matches against an {@link HttpRequest} method (via a {@link MethodRequestMatcher}) and it's path.
 *
 * @since 4.0
 */
@NoImplement
public interface PathAndMethodRequestMatcher extends RequestMatcher {

  /**
   * @return a fresh {@link PathAndMethodRequestMatcherBuilder}
   * @since 4.1.5
   */
  static PathAndMethodRequestMatcherBuilder builder() {
    return new PathAndMethodRequestMatcherBuilder();
  }

  /**
   * @return the path to match
   */
  String getPath();

  /**
   * @return the {@link MethodRequestMatcher} to use
   */
  MethodRequestMatcher getMethodRequestMatcher();

}
