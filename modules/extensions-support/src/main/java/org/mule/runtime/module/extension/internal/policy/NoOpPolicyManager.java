/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.policy;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.policy.MessageSourceResponseParametersProcessor;
import org.mule.runtime.core.internal.policy.NoSourcePolicy;
import org.mule.runtime.core.internal.policy.OperationParametersProcessor;
import org.mule.runtime.core.internal.policy.OperationPolicy;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.internal.policy.SourcePolicy;
import org.mule.runtime.policy.api.PolicyPointcutParameters;

/**
 * Implementation of {@link PolicyManager} that does not apply any policy.
 *
 * @since 4.3
 */
public class NoOpPolicyManager implements PolicyManager {

  private static final OperationPolicy NO_POLICY_OPERATION =
      (operationEvent, operationExecutionFunction, opParamProcessor, componentLocation, callback) -> operationExecutionFunction
          .execute(opParamProcessor.getOperationParameters(), operationEvent, callback);

  @Override
  public SourcePolicy createSourcePolicyInstance(Component source, CoreEvent sourceEvent,
                                                 ReactiveProcessor flowExecutionProcessor,
                                                 MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
    return new NoSourcePolicy(flowExecutionProcessor);
  }

  @Override
  public PolicyPointcutParameters addSourcePointcutParametersIntoEvent(Component source, TypedValue<?> attributes,
                                                                       InternalEvent event) {
    return new PolicyPointcutParameters(source);
  }

  @Override
  public OperationPolicy createOperationPolicy(Component operation, CoreEvent operationEvent,
                                               OperationParametersProcessor operationParameters) {
    return NO_POLICY_OPERATION;
  }

}
