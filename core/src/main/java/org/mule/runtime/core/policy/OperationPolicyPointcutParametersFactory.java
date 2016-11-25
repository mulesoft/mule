/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.Map;

public interface OperationPolicyPointcutParametersFactory {

  /**
   * @return true if this factory can create {@link PolicyPointcutParameters} for the operation identifier, false otherwise.
   */
  boolean supportsOperationIdentifier(ComponentIdentifier operationIdentifier);

  /**
   * Creates an specific {@link PolicyPointcutParameters} for a particular operation identifier by {@code operationIdentifier}.
   *
   * @param operationIdentifier identifier of the message source
   * @param operationParameters set of parameters that are going to be used to execute the operation.
   * @return the pointcut parameters.
   */
  PolicyPointcutParameters createPolicyPointcutParameters(ComponentIdentifier operationIdentifier,
                                                          Map<String, Object> operationParameters);

}
