/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server;

import org.mule.runtime.http.api.domain.message.request.HttpRequest;

/**
 * {@link RequestMatcher} that matches against an {@link HttpRequest} method (via a {@link MethodRequestMatcher}) and it's path.
 *
 * @since 4.0
 */
public interface PathAndMethodRequestMatcher extends RequestMatcher {

  /**
   * @return the path to match
   */
  String getPath();

  /**
   * @return the {@link MethodRequestMatcher} to use
   */
  MethodRequestMatcher getMethodRequestMatcher();
}
