/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.policy.api;

import org.mule.runtime.api.component.location.ComponentLocation;
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
   * @param componentLocation the component location where the source / operation is defined.
   * @param path the target path of the message
   * @param method the HTTP method of the message
   */
  public HttpPolicyPointcutParameters(ComponentLocation componentLocation, String path, String method) {
    super(componentLocation);
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
