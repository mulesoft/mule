/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.policy.PolicyStateId;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.function.BiConsumer;

import org.junit.Test;
import org.mockito.Mockito;

public class DefaultPolicyStateHandlerTestCase extends AbstractMuleTestCase {


  private static final String TEST_EXECUTION_ID = "test-id";
  private static final String TEST_EXECUTION_ID2 = "test-id2";
  private static final String TEST_POLICY_ID = "test-policy-id";
  private static final String TEST_POLICY_ID2 = "test-policy-id2";

  private InternalEvent eventTestExecutionId = mock(InternalEvent.class, RETURNS_DEEP_STUBS);
  private InternalEvent eventTestExecutionId2 = mock(InternalEvent.class, RETURNS_DEEP_STUBS);

  private DefaultPolicyStateHandler defaultPolicyStateHandler = new DefaultPolicyStateHandler();

  @Test
  public void destroyStateWithNoData() {
    defaultPolicyStateHandler.destroyState(TEST_EXECUTION_ID);
  }

  @Test
  public void samePolicyDifferentExecutionId() {
    PolicyStateId policyStateExecutionId = new PolicyStateId(TEST_EXECUTION_ID, TEST_POLICY_ID);
    PolicyStateId policyStateExecutionId2 = new PolicyStateId(TEST_EXECUTION_ID2, TEST_POLICY_ID);
    defaultPolicyStateHandler.updateState(policyStateExecutionId, eventTestExecutionId);
    defaultPolicyStateHandler.updateState(policyStateExecutionId2, eventTestExecutionId2);
    assertThat(defaultPolicyStateHandler.getLatestState(policyStateExecutionId).get(), is(eventTestExecutionId));
    assertThat(defaultPolicyStateHandler.getLatestState(policyStateExecutionId2).get(), is(eventTestExecutionId2));
  }

  @Test
  public void sameExecutionDifferentPolicyId() {
    PolicyStateId policy1StateExecutionId = new PolicyStateId(TEST_EXECUTION_ID, TEST_POLICY_ID);
    PolicyStateId policy2StateExecutionId = new PolicyStateId(TEST_EXECUTION_ID, TEST_POLICY_ID2);
    defaultPolicyStateHandler.updateState(policy1StateExecutionId, eventTestExecutionId);
    defaultPolicyStateHandler.updateState(policy2StateExecutionId, eventTestExecutionId2);
    assertThat(defaultPolicyStateHandler.getLatestState(policy1StateExecutionId).get(), is(eventTestExecutionId));
    assertThat(defaultPolicyStateHandler.getLatestState(policy2StateExecutionId).get(), is(eventTestExecutionId2));
  }

  @Test
  public void destroyState() {
    PolicyStateId policyStateExecutionId = new PolicyStateId(TEST_EXECUTION_ID, TEST_POLICY_ID);
    defaultPolicyStateHandler.destroyState(policyStateExecutionId.getExecutionIdentifier());
    assertThat(defaultPolicyStateHandler.getLatestState(policyStateExecutionId).isPresent(), is(false));
    assertThat(defaultPolicyStateHandler.retrieveNextOperation(policyStateExecutionId.getExecutionIdentifier()), nullValue());
  }

  @Test
  public void destroyStateWhenEventIsCompleted() {
    PolicyStateId policyStateExecutionId = new PolicyStateId(TEST_EXECUTION_ID, TEST_POLICY_ID);
    Reference<BiConsumer> subscriberReference = new Reference<>();
    BaseEventContext rootEventContext = eventTestExecutionId.getContext().getRootContext();
    Mockito.doAnswer(invocationOnMock -> {
      subscriberReference.set((BiConsumer) invocationOnMock.getArguments()[0]);
      return null;
    }).when(rootEventContext).onTerminated(any(BiConsumer.class));
    defaultPolicyStateHandler.updateState(policyStateExecutionId, eventTestExecutionId);
    subscriberReference.get().accept(null, null);
    assertThat(defaultPolicyStateHandler.getLatestState(policyStateExecutionId).isPresent(), is(false));
    assertThat(defaultPolicyStateHandler.policyStateIdsByExecutionIdentifier
        .containsKey(policyStateExecutionId.getExecutionIdentifier()), is(false));
    assertThat(defaultPolicyStateHandler.nextOperationMap.containsKey(policyStateExecutionId.getExecutionIdentifier()),
               is(false));
    assertThat(defaultPolicyStateHandler.stateMap.containsKey(policyStateExecutionId), is(false));
  }

}
