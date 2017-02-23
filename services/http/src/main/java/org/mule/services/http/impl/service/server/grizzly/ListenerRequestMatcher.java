/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.server.grizzly;

import static java.lang.String.format;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.server.MethodRequestMatcher;
import org.mule.service.http.api.server.PathAndMethodRequestMatcher;
import org.mule.service.http.api.server.RequestMatcher;

import java.util.Objects;

/**
 * {@link RequestMatcher} for an HTTP listener that accepts request based on a path and a group of allowed methods.
 */
public class ListenerRequestMatcher implements PathAndMethodRequestMatcher {

  private final String path;
  private final MethodRequestMatcher methodRequestMatcher;

  public ListenerRequestMatcher(final MethodRequestMatcher methodRequestMatcher, final String path) {
    this.methodRequestMatcher = methodRequestMatcher;
    this.path = endsWithWildcardPath(path) ? path : path + "/";
  }

  private boolean endsWithWildcardPath(final String path) {
    return path.endsWith("/") || path.endsWith("*");
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public MethodRequestMatcher getMethodRequestMatcher() {
    return methodRequestMatcher;
  }

  @Override
  public boolean matches(HttpRequest request) {
    return methodRequestMatcher.matches(request);
  }

  @Override
  public String toString() {
    return format("%s{path='%s', methodRequestMatcher='%s'}", this.getClass().getSimpleName(), path, methodRequestMatcher);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path.hashCode(), methodRequestMatcher.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ListenerRequestMatcher
        && Objects.equals(path, ((ListenerRequestMatcher) obj).path)
        && Objects.equals(methodRequestMatcher, ((ListenerRequestMatcher) obj).methodRequestMatcher);
  }
}
