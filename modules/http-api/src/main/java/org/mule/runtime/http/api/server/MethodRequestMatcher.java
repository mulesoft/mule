/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server;

import org.mule.runtime.http.api.domain.message.request.HttpRequest;

import java.util.List;

/**
 * {@link RequestMatcher} that specifically matches against an {@link HttpRequest} method.
 *
 * @since 4.0
 */
public interface MethodRequestMatcher extends RequestMatcher {

  /**
   * @param matcher another {@link MethodRequestMatcher}.
   * @return true if this and {@code matcher} have matching methods in common, false otherwise.
   */
  boolean intersectsWith(MethodRequestMatcher matcher);

  /**
   * @return the list of methods to match.
   */
  List<String> getMethods();

}
