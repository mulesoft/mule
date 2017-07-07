/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.processor.Processor;

/**
 * Factory for {@link Processor} instances created from a {@link Policy}
 *
 * @since 4.0
 */
public interface SourcePolicyProcessorFactory {

  /**
   * Creates an {@link SourcePolicy}.
   *
   * @param policy the policy from which the {@link SourcePolicy} gets created.
   * @param nextProcessor the next processor in the chain.
   * @return an {@link SourcePolicy} that performs the common logic related to policies.
   */
  Processor createSourcePolicy(Policy policy, Processor nextProcessor);

}
