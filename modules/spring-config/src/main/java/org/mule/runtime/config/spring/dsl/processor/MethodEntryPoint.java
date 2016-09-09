/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

/**
 * Object that represents the configuration to be used to resolve which method should be executed in a
 * {@link org.mule.runtime.core.api.component.Component}.
 *
 * @since 4.0
 */
public class MethodEntryPoint {

  private boolean enabled;
  private String method;

  /**
   * {@see setMethod}
   */
  public String getMethod() {
    return method;
  }

  /**
   * @param method name of the method
   */
  public void setMethod(String method) {
    this.method = method;
  }

  /**
   * {@see setEnabled}
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * @param enabled if the method should be consider or not as a possible entry point of a
   *        {@link org.mule.runtime.core.api.component.Component}
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
