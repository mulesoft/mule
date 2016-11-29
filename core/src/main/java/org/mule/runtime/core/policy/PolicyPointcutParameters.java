/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.dsl.api.component.ComponentIdentifier;

/**
 * This class defines a set of parameters related to the {@link org.mule.runtime.core.api.source.MessageSource} and the message
 * received by the {@link org.mule.runtime.core.api.source.MessageSource}. These parameters will later by used during policy
 * lookup in {@link PolicyProvider} to match it against a configured pointcut.
 * 
 * @since 4.0
 */
public class PolicyPointcutParameters {

  private final ComponentIdentifier componentIdentifier;

  /**
   * Creates a new {@link PolicyPointcutParameters}
   * 
   * @param componentIdentifier the component identifier. This is the namespace of the module were it is defined and the source /
   *        operation identifier.
   */
  public PolicyPointcutParameters(ComponentIdentifier componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
  }

  /**
   * @return the component identifier. This is the namespace of the module were it is defined and the source / operation
   *         identifier.
   */
  public final ComponentIdentifier getComponentIdentifier() {
    return componentIdentifier;
  }

}
