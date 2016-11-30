/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.server;

import org.mule.service.http.api.domain.request.HttpRequest;

import java.util.List;

/**
 * {@link RequestMatcher} that specifically matches against an {@link HttpRequest} method.
 *
 * @since 4.0
 */
public interface MethodRequestMatcher extends RequestMatcher {

  /**
   * @param otherMatcher another {@link MethodRequestMatcher}.
   * @return true if this and {@code otherMatcher} have matching methods in common, false otherwise.
   */
  boolean intersectsWith(MethodRequestMatcher otherMatcher);

  /**
   * @return the list of methods to match.
   */
  List<String> getMethods();

}
