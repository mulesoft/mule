/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.server;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.http.api.HttpConstants.Method;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Builder of {@link MethodRequestMatcher}. At the very least one HTTP method must be added and instances can only be obtained via
 * {@link MethodRequestMatcher#builder()} or {@link MethodRequestMatcher#builder(Collection)}.
 *
 * @since 4.1.5
 */
public class MethodRequestMatcherBuilder {

  private final Collection<String> methods = new LinkedList<>();

  MethodRequestMatcherBuilder() {}

  MethodRequestMatcherBuilder(Collection<String> methods) {
    checkArgument(methods != null, "methods attribute should not be null");
    methods.forEach(method -> this.methods.add(method.toUpperCase()));
  }

  /**
   * Adds a new method to match against.
   *
   * @param method a {@link String} representation of an HTTP method
   * @return this builder
   */
  public MethodRequestMatcherBuilder add(String method) {
    checkArgument(method != null, "method attribute should not be null");
    this.methods.add(method.toUpperCase());
    return this;
  }

  /**
   * Adds a new method to match against.
   *
   * @param method a {@link Method}
   * @return this builder
   */
  public MethodRequestMatcherBuilder add(Method method) {
    checkArgument(method != null, "method attribute should not be null");
    return this.add(method.name());
  }

  /**
   * @return a {@link MethodRequestMatcher} configured as desired
   */
  public MethodRequestMatcher build() {
    checkArgument(methods.size() > 0, "methods attribute should not be empty");
    return new DefaultMethodRequestMatcher(methods);
  }

}
