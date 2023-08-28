/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.server;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.http.api.server.MethodRequestMatcher.acceptAll;

/**
 * Builder of {@link PathAndMethodRequestMatcher}. At the very least a path should be selected since by default a
 * {@link MethodRequestMatcher#acceptAll()} matcher will be used. Instances can only be obtained via
 * {@link PathAndMethodRequestMatcher#builder()}.
 *
 * @since 4.1.5
 */
public class PathAndMethodRequestMatcherBuilder {

  private String path;
  private MethodRequestMatcher methodRequestMatcher = acceptAll();

  PathAndMethodRequestMatcherBuilder() {}

  /**
   * Selects a {@link MethodRequestMatcher} to use. {@link MethodRequestMatcher#acceptAll()} is used by default.
   *
   * @param methodRequestMatcher the {@link MethodRequestMatcher} to use
   * @return this builder
   */
  public PathAndMethodRequestMatcherBuilder methodRequestMatcher(MethodRequestMatcher methodRequestMatcher) {
    checkArgument(methodRequestMatcher != null, "method matcher cannot be null");
    this.methodRequestMatcher = methodRequestMatcher;
    return this;
  }

  /**
   * Selects the path pattern to use. Must be defined.
   *
   * @param path the path to match against, which could contain wildcards and parametrization
   * @return
   */
  public PathAndMethodRequestMatcherBuilder path(String path) {
    checkArgument(nonEmpty(path), "path cannot be empty nor null");
    this.path = path;
    return this;
  }

  private boolean nonEmpty(String path) {
    return path != null && !"".equals(path.trim());
  }

  /**
   * @return a {@link PathAndMethodRequestMatcher} configured as desired.
   */
  public PathAndMethodRequestMatcher build() {
    return new DefaultPathAndMethodRequestMatcher(methodRequestMatcher, path);
  }

}
