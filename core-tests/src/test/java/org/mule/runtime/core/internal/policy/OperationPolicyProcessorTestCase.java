/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.policy;

import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.processor.ReactiveProcessor;

public class OperationPolicyProcessorTestCase extends AbstractPolicyProcessorTestCase {

  @Override
  protected ReactiveProcessor getProcessor() {
    when(policy.getPolicyId()).thenReturn("id");
    return new OperationPolicyProcessor(policy, flowProcessor);
  }
}
