/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.policy.api;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.Component;

import java.util.Map;

/**
 * Factory for instances of {@link PolicyPointcutParameters} for a particular operation.
 *
 * Instances of this factory must be registered in the application's context and will be discovered and used to create the set of
 * pointcut parameters for an operation before executing it.
 * 
 * @since 4.0
 */
public interface OperationPolicyPointcutParametersFactory {

  /**
   * @return true if this factory can create {@link PolicyPointcutParameters} for the operation identifier, false otherwise.
   */
  boolean supportsOperationIdentifier(ComponentIdentifier operationIdentifier);

  /**
   * Creates an specific {@link PolicyPointcutParameters} for a particular operation identifier by {@code operationIdentifier}.
   *
   * @param operation the operation where the policy is being applied.
   * @param operationParameters set of parameters that are going to be used to execute the operation.
   * @return the pointcut parameters.
   */
  PolicyPointcutParameters createPolicyPointcutParameters(Component operation,
                                                          Map<String, Object> operationParameters);

}
