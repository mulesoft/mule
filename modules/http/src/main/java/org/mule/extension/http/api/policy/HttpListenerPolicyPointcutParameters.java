/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.policy;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.core.policy.PolicyPointcutParameters;

/**
 * Specific implementation of {@link PolicyPointcutParameters} for http:listener operation.
 * 
 * @since 4.0
 */
public class HttpListenerPolicyPointcutParameters extends PolicyPointcutParameters {

  private final String path;
  private final String method;

  /**
   * Creates a new {@link PolicyPointcutParameters}
   *
   * @param flowName name of the flow where the listener is defined. Not empty.
   * @param componentIdentifier the component identifier. This is the namespace of the module were it is defined and the source /
   *        operation identifier.
   * @param path the target path of the incoming request
   * @param method the HTTP method of the incoming request
   */
  public HttpListenerPolicyPointcutParameters(String flowName, ComponentIdentifier componentIdentifier, String path,
                                              String method) {
    super(flowName, componentIdentifier);
    this.path = path;
    this.method = method;
  }

  /**
   * @return the target path of the http:request operation.
   */
  public String getPath() {
    return path;
  }

  /**
   * @return the HTTP method of the http:request operation.
   */
  public String getMethod() {
    return method;
  }

}
