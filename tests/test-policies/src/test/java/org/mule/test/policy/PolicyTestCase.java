/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.policy;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.internal.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyInstance;
import org.mule.runtime.core.internal.policy.PolicyProvider;
import org.mule.runtime.policy.api.PolicyPointcutParameters;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class PolicyTestCase extends MuleArtifactFunctionalTestCase {

  private static final String POLICY_ID = "policyId";

  @Override
  protected String getConfigFile() {
    return "test-policy-config.xml";
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(customPolicyProviderConfigurationBuilder());
  }

  private ConfigurationBuilder customPolicyProviderConfigurationBuilder() {
    return new ConfigurationBuilder() {

      @Override
      public void configure(MuleContext muleContext) throws ConfigurationException {
        final AtomicReference<Optional<PolicyInstance>> policyReference = new AtomicReference<>();
        muleContext.getCustomizationService().registerCustomServiceImpl("customPolicyProvider",
                                                                        new TestPolicyProvider(policyReference, muleContext));
      }

      @Override
      public boolean isConfigured() {
        return false;
      }
    };
  }

  @Test
  public void parsesPolicy() throws Exception {
    PolicyProvider policyProvider = muleContext.getRegistry().lookupObject(PolicyProvider.class);

    assertThat(policyProvider, instanceOf(TestPolicyProvider.class));
    TestPolicyProvider testPolicyProvider = (TestPolicyProvider) policyProvider;
    assertThat(testPolicyProvider.policyReference.get(), is(not(nullValue())));

    List<Policy> sourceParameterizedPolicies = testPolicyProvider.findSourceParameterizedPolicies(null);
    assertThat(sourceParameterizedPolicies.size(), equalTo(1));

    List<Policy> operationParameterizedPolicies = testPolicyProvider.findOperationParameterizedPolicies(null);
    assertThat(operationParameterizedPolicies.size(), equalTo(1));
  }

  private static class TestPolicyProvider implements PolicyProvider, Initialisable {

    private final AtomicReference<Optional<PolicyInstance>> policyReference;
    private final MuleContext muleContext;

    public TestPolicyProvider(AtomicReference<Optional<PolicyInstance>> policyReference, MuleContext muleContext) {
      this.policyReference = policyReference;
      this.muleContext = muleContext;
    }

    @Override
    public List<Policy> findSourceParameterizedPolicies(PolicyPointcutParameters policyPointcutParameters) {
      List<Policy> policies = policyReference.get().map(policy -> policy.getSourcePolicyChain()
          .map(sourceChain -> asList(new Policy(sourceChain, POLICY_ID)))
          .orElse(emptyList())).orElse(emptyList());

      return policies;
    }

    @Override
    public List<Policy> findOperationParameterizedPolicies(PolicyPointcutParameters policyPointcutParameters) {

      List<Policy> policies = policyReference.get().map(policy -> policy.getOperationPolicyChain()
          .map(operationChain -> asList(new Policy(operationChain, "policyId")))
          .orElse(emptyList())).orElse(emptyList());

      return policies;
    }

    private PolicyInstance getPolicyFromRegistry() {
      try {
        PolicyInstance policyInstance = muleContext.getRegistry().lookupObject(PolicyInstance.class);
        policyInstance.initialise();
        policyInstance.start();
        return policyInstance;
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    }

    @Override
    public void initialise() throws InitialisationException {
      if (policyReference.get() == null) {
        PolicyInstance policyInstance = getPolicyFromRegistry();
        policyReference.set(ofNullable(policyInstance));
      }
    }
  }
}
