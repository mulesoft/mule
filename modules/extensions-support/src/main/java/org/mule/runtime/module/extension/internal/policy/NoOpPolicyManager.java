/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.policy;

import static org.mule.runtime.core.internal.policy.DefaultPolicyManager.noPolicyOperation;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.policy.DefaultPolicyManager;
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
    return noPolicyOperation();
  }

}
