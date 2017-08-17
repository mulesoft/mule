/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.policy.api;

import org.mule.runtime.api.component.location.ComponentLocation;

/**
 * This class defines a set of parameters related to the execution of a component. In particular to the execution of a source or
 * an operation.
 * 
 * These parameters will later by used during policy lookup to match it against a configured pointcut.
 * 
 * @since 4.0
 */
public class PolicyPointcutParameters {

  private final ComponentLocation componentLocation;

  /**
   * Creates a new {@link PolicyPointcutParameters}
   *
   * @param componentLocation the component location where the source / operation is defined.
   */
  public PolicyPointcutParameters(ComponentLocation componentLocation) {
    this.componentLocation = componentLocation;
  }

  /**
   * @return the component location where the source / operation is defined.
   */
  public final ComponentLocation getComponentLocation() {
    return componentLocation;
  }

}
