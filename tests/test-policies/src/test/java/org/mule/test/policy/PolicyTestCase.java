/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import org.mule.runtime.core.api.policy.Policy;

import java.util.List;

import org.junit.Test;

public class PolicyTestCase extends AbstractPolicyTestCase {

  @Override
  protected String getConfigFile() {
    return "test-policy-config.xml";
  }

  @Test
  public void parsesPolicy() throws Exception {
    assertThat(policyProvider, instanceOf(TestPolicyProvider.class));
    TestPolicyProvider testPolicyProvider = (TestPolicyProvider) policyProvider;
    assertThat(testPolicyProvider.policyReference.get(), is(not(nullValue())));

    List<Policy> sourceParameterizedPolicies = testPolicyProvider.findSourceParameterizedPolicies(null);
    assertThat(sourceParameterizedPolicies.size(), equalTo(1));

    List<Policy> operationParameterizedPolicies = testPolicyProvider.findOperationParameterizedPolicies(null);
    assertThat(operationParameterizedPolicies.size(), equalTo(1));
  }

  @Test
  public void policyDisposalStopsSchedulers() {
    // Once the application is started with it's corresponding test policy. Test whether the policy-specific schedulers, generated
    // in the PolicyProcessingStrategy, are stopped once the whole application is
    // disposed.
    muleContext.dispose();
    assertThat(schedulerService.getSchedulers().size(), is(0));
  }
}
