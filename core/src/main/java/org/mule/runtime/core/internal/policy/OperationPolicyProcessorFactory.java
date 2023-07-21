/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

/**
 * Factory for {@link OperationPolicy} instances.
 *
 * @since 4.0
 */
public interface OperationPolicyProcessorFactory {

  /**
   * Creates a {@link Processor} to execute the {@code policy}.
   *
   * @param policy        the policy from which the {@link OperationPolicy} gets created.
   * @param nextProcessor the next-operation processor implementation
   *
   * @return an {@link OperationPolicy} that performs the common logic related to policies.
   */
  ReactiveProcessor createOperationPolicy(Policy policy, ReactiveProcessor nextProcessor);

}
