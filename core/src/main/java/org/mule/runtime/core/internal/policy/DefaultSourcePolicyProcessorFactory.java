/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
