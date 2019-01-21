/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.processor.Processor;

import static org.mockito.Mockito.when;

public class OperationPolicyProcessorTestCase extends AbstractPolicyProcessorTestCase {

  @Override
  protected Processor getProcessor() {
    when(policy.getPolicyId()).thenReturn("id");
    return new OperationPolicyProcessor(policy, policyStateHandler, flowProcessor);
  }
}
