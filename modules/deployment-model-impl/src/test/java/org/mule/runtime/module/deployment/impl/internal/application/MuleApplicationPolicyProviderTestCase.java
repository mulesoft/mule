/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.application;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.core.policy.Policy;
import org.mule.runtime.core.policy.PolicyParametrization;
import org.mule.runtime.core.policy.PolicyPointcut;
import org.mule.runtime.core.policy.PolicyPointcutParameters;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyInstanceProvider;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyInstanceProviderFactory;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class MuleApplicationPolicyProviderTestCase extends AbstractMuleTestCase {


  private final PolicyInstanceProviderFactory policyInstanceProviderFactory = mock(PolicyInstanceProviderFactory.class);
  private final PolicyTemplateFactory policyTemplateFactory = mock(PolicyTemplateFactory.class);
  private final MuleApplicationPolicyProvider policyProvider =
      new MuleApplicationPolicyProvider(policyTemplateFactory, policyInstanceProviderFactory);
  private final Application application = mock(Application.class);
  private final PolicyPointcut pointcut = mock(PolicyPointcut.class);
  private final PolicyParametrization parametrization = new PolicyParametrization("policyId", pointcut, emptyMap());
  private final PolicyTemplateDescriptor policyTemplateDescriptor = new PolicyTemplateDescriptor("testPolicy");
  private final PolicyPointcutParameters policyPointcutParameters = mock(PolicyPointcutParameters.class);
  private PolicyTemplate policyTemplate = mock(PolicyTemplate.class);
  private final PolicyInstanceProvider policyInstanceProvider = mock(PolicyInstanceProvider.class);
  private final Policy policy1 = mock(Policy.class);
  private final Policy policy2 = mock(Policy.class);
  private final List<Policy> policiesToApply = new ArrayList<>();

  @Before
  public void setUp() throws Exception {
    policyProvider.setApplication(application);
    when(policyTemplateFactory.createArtifact(policyTemplateDescriptor, null)).thenReturn(policyTemplate);
    when(policyInstanceProvider.getPointcut()).thenReturn(pointcut);
    when(policyInstanceProvider.findOperationParameterizedPolicies(policyPointcutParameters)).thenReturn(policiesToApply);
    when(policyInstanceProvider.findSourceParameterizedPolicies(policyPointcutParameters)).thenReturn(policiesToApply);
    when(policyInstanceProviderFactory.create(application, policyTemplate, parametrization)).thenReturn(policyInstanceProvider);
  }

  @Test
  public void addsOperationPoliciesFromSinglePolicyInstance() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true);

    policiesToApply.add(policy1);
    policiesToApply.add(policy2);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(2));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
  }

  @Test
  public void addsOperationPoliciesFromSinglePolicyInstanceApplyingPointCut() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false);

    policiesToApply.add(policy1);
    policiesToApply.add(policy2);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(0));
  }

  @Test
  public void addsOperationPoliciesFromMultiplePolicyInstances() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(true);

    policiesToApply.add(policy1);
    policiesToApply.add(policy2);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(4));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
    assertThat(parameterizedPolicies.get(2), is(policy1));
    assertThat(parameterizedPolicies.get(3), is(policy2));
  }

  @Test
  public void addsOperationPoliciesFromFirstPolicyInstance() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(false);
    doMultipleOperationPoliciesPoincutRejectionTest();
  }

  @Test
  public void addsOperationPoliciesFromSecondPolicyInstance() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false).thenReturn(true);
    doMultipleOperationPoliciesPoincutRejectionTest();
  }

  private void doMultipleOperationPoliciesPoincutRejectionTest() {
    policiesToApply.add(policy1);
    policiesToApply.add(policy2);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(2));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
  }


  @Test
  public void addsOperationPoliciesFromMultiplePolicyInstancesApplyingPointCuts() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false).thenReturn(false);

    policiesToApply.add(policy1);
    policiesToApply.add(policy2);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(0));
  }

  @Test
  public void addsSourcePoliciesFromSinglePolicyInstance() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true);

    policiesToApply.add(policy1);
    policiesToApply.add(policy2);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(2));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
  }

  @Test
  public void addsSourcePoliciesFromSinglePolicyInstanceApplyingPointCut() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false);

    policiesToApply.add(policy1);
    policiesToApply.add(policy2);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(0));
  }

  @Test
  public void addsSourcePoliciesFromMultiplePolicyInstances() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(true);

    policiesToApply.add(policy1);
    policiesToApply.add(policy2);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(4));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
    assertThat(parameterizedPolicies.get(2), is(policy1));
    assertThat(parameterizedPolicies.get(3), is(policy2));
  }

  @Test
  public void addsSourcePoliciesFromFirstPolicyInstance() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(false);
    doMultipleSourcePoliciesPoincutRejectionTest();
  }

  @Test
  public void addsSourcePoliciesFromSecondPolicyInstance() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false).thenReturn(true);
    doMultipleSourcePoliciesPoincutRejectionTest();
  }

  private void doMultipleSourcePoliciesPoincutRejectionTest() {
    policiesToApply.add(policy1);
    policiesToApply.add(policy2);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(2));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
  }

  @Test
  public void addsSourcePoliciesFromMultiplePolicyInstancesApplyingPointCuts() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false).thenReturn(false);

    policiesToApply.add(policy1);
    policiesToApply.add(policy2);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(0));
  }
}
