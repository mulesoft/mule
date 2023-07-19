/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

/**
 * Default implementation for {@link SourcePolicyProcessorFactory}.
 *
 * @since 4.0
 */
public class DefaultSourcePolicyProcessorFactory implements SourcePolicyProcessorFactory {

  @Override
  public ReactiveProcessor createSourcePolicy(Policy policy, ReactiveProcessor nextProcessor) {
    return new SourcePolicyProcessor(policy, nextProcessor);
  }
}
