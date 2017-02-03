/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.policy;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.message.Message;

import java.util.Map;

/**
 * Implementations of this interface must provide a transformation between the parameters of an operation and a {@link Message}.
 * Such transformation is used to be able to execute the policy pipeline and handle the information to be sent by the operation or
 * modify it.
 *
 * @since 4.0
 */
public interface OperationPolicyParametersTransformer {

  /**
   * @param componentIdentifier the operation identifier.
   * @return true if this implementation supports the specified component, false otherwise.
   */
  boolean supports(ComponentIdentifier componentIdentifier);

  /**
   * Transforms a set of parameters to a message that can be route through the policy pipeline.
   *
   * Such transformation must be done taking into account that all the useful information from the parameters must be accessible
   * through the created {@link Message}.
   *
   * @param parameters resolved set of parameters to be processed by the operation.
   * @return a new {@link Message} with all the useful content of the parameters.
   */
  Message fromParametersToMessage(Map<String, Object> parameters);

  /**
   * Transformers the output of the operation policy pipeline to the set of parameters to be sent by the operation.
   *
   * @param message the output message from the policy source pipeline.
   * @return a set of parameters resolved from the message of the policy operation pipeline.
   */
  Map<String, Object> fromMessageToParameters(Message message);

}
