/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.policy;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.http.policy.api.HttpPolicyPointcutParameters;
import org.mule.runtime.policy.api.PolicyPointcutParameters;

/**
 * Specific implementation of {@link PolicyPointcutParameters} for http:request operation.
 * 
 * @since 4.0
 */
public class HttpRequestPolicyPointcutParameters extends HttpPolicyPointcutParameters {

  /**
   * Creates a new {@link PolicyPointcutParameters}
   *
   * @param componentLocation the component location where the source / operation is defined.
   * @param path the target path of the http:request operation.
   * @param method the HTTP method of the http:request operation.
   */
  public HttpRequestPolicyPointcutParameters(ComponentLocation componentLocation, String path, String method) {
    super(componentLocation, path, method);
  }

}
