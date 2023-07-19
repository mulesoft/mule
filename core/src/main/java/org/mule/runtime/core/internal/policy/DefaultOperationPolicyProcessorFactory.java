/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

/**
 * Default implementation for {@link OperationPolicyProcessorFactory}.
 *
 * @since 4.0
 */
public class DefaultOperationPolicyProcessorFactory implements OperationPolicyProcessorFactory {

  @Override
  public ReactiveProcessor createOperationPolicy(Policy policy, ReactiveProcessor nextProcessor) {
    return new OperationPolicyProcessor(policy, nextProcessor);
  }
}
