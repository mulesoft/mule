/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener.matcher;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.server.MethodRequestMatcher;

import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

/**
 * Request matcher for http methods.
 */
public class DefaultMethodRequestMatcher implements MethodRequestMatcher {

  private final List<String> methods;

  /**
   * The list of methods accepted by this matcher
   *
   * @param methods http request method allowed.
   */
  public DefaultMethodRequestMatcher(final String... methods) {
    checkArgument(methods != null, "methods attribute should not be null");
    this.methods = (List<String>) CollectionUtils.collect(Arrays.asList(methods), new Transformer() {

      @Override
      public Object transform(Object input) {
        return ((String) input).toLowerCase();
      }
    });
  }

  /**
   * Determines if there's an intersection between the allowed methods by two request matcher
   *
   * @param matcher request matcher to test against
   * @return true at least there's one http method that both request matcher accepts, false otherwise.
   */
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
    return this.methods.contains(httpRequest.getMethod().toLowerCase());
  }

  @Override
  public String toString() {
    return "MethodRequestMatcher{" + "methods=" + getMethodsListRepresentation(this.methods) + '}';
  }

  @Override
  public List<String> getMethods() {
    return ImmutableList.copyOf(methods);
  }

  public static String getMethodsListRepresentation(List<String> methods) {
    return methods.isEmpty() ? "*" : Arrays.toString(methods.toArray());
  }
}
