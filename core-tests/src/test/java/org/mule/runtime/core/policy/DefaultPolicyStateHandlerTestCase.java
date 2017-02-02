/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.mule.runtime.core.api.Event;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class DefaultPolicyStateHandlerTestCase extends AbstractMuleTestCase {


  private static final String TEST_EXECUTION_ID = "test-id";
  private static final String TEST_EXECUTION_ID2 = "test-id2";
  private static final String TEST_POLICY_ID = "test-policy-id";
  private static final String TEST_POLICY_ID2 = "test-policy-id2";

  private Event eventTestExecutionId = mock(Event.class);
  private Event eventTestExecutionId2 = mock(Event.class);

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
    defaultPolicyStateHandler.destroyState(policyStateExecutionId.getExecutionIndentifier());
    assertThat(defaultPolicyStateHandler.getLatestState(policyStateExecutionId).isPresent(), is(false));
    assertThat(defaultPolicyStateHandler.retrieveNextOperation(policyStateExecutionId.getExecutionIndentifier()), nullValue());
  }

}
