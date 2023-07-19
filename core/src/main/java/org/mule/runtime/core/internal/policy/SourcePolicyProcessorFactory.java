/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

/**
 * Factory for {@link Processor} instances created from a {@link Policy}
 *
 * @since 4.0
 */
public interface SourcePolicyProcessorFactory {

  /**
   * Creates an {@link SourcePolicy}.
   *
   * @param policy        the policy from which the {@link SourcePolicy} gets created.
   * @param nextProcessor the next processor in the chain.
   * @return an {@link SourcePolicy} that performs the common logic related to policies.
   */
  ReactiveProcessor createSourcePolicy(Policy policy, ReactiveProcessor nextProcessor);

}
