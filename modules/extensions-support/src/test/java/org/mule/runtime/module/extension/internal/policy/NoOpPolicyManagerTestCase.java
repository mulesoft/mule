/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.internal.policy.DefaultPolicyManager.noPolicyOperation;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.policy.OperationParametersProcessor;
import org.mule.runtime.core.internal.policy.OperationPolicy;
import org.mule.tck.junit4.AbstractMuleTestCase;

import io.qameta.allure.Issue;
import org.junit.Test;

public class NoOpPolicyManagerTestCase extends AbstractMuleTestCase {

  private NoOpPolicyManager noOpPolicyManager = new NoOpPolicyManager();

  @Test
  @Issue("MULE-18442")
  public void noOpPolicyManagerCreatesNoPolicyOperation() {
    Component operation = mock(Component.class);
    CoreEvent operationEvent = mock(CoreEvent.class);
    OperationParametersProcessor operationParameters = mock(OperationParametersProcessor.class);

    OperationPolicy policy = noOpPolicyManager.createOperationPolicy(operation, operationEvent, operationParameters);

    assertThat(policy, is(noPolicyOperation()));
  }
}
