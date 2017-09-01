/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.policy.api;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.policy.api.PolicyPointcutParameters;

/**
 * Specific implementation of {@link PolicyPointcutParameters} for HTTP.
 *
 * @since 4.0
 */
public abstract class HttpPolicyPointcutParameters extends PolicyPointcutParameters {

  private final String path;
  private final String method;

  /**
   * Creates a new {@link PolicyPointcutParameters}
   *
   * @param component the component where the policy is being applied.
   * @param path the target path of the message
   * @param method the HTTP method of the message
   */
  public HttpPolicyPointcutParameters(Component component, String path, String method) {
    super(component);
    this.path = path;
    this.method = method;
  }

  /**
   * @return the target path of the http message.
   */
  public String getPath() {
    return path;
  }

  /**
   * @return the HTTP method of the http message.
   */
  public String getMethod() {
    return method;
  }

}
