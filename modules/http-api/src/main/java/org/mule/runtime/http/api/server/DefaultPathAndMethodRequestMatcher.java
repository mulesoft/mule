/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.server;

import static java.lang.String.format;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;

import java.util.Objects;

/**
 * {@link RequestMatcher} that accepts request based on a path and a group of allowed methods.
 */
class DefaultPathAndMethodRequestMatcher implements PathAndMethodRequestMatcher {

  private final String path;
  private final MethodRequestMatcher methodRequestMatcher;

  DefaultPathAndMethodRequestMatcher(final MethodRequestMatcher methodRequestMatcher, final String path) {
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
    return obj instanceof DefaultPathAndMethodRequestMatcher
        && Objects.equals(path, ((DefaultPathAndMethodRequestMatcher) obj).path)
        && Objects.equals(methodRequestMatcher, ((DefaultPathAndMethodRequestMatcher) obj).methodRequestMatcher);
  }

}
