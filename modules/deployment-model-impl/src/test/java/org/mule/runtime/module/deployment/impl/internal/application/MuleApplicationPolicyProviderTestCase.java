/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.application;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.deployment.impl.internal.application.MuleApplicationPolicyProvider.createPolicyRegistrationError;

import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.core.api.policy.PolicyPointcut;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.policy.PolicyRegistrationException;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.deployment.impl.internal.policy.ApplicationPolicyInstance;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyInstanceProviderFactory;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateFactory;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class MuleApplicationPolicyProviderTestCase extends AbstractMuleTestCase {

  private static final String POLICY_NAME1 = "testPolicy1";
  private static final String POLICY_NAME2 = "testPolicy2";
  private static final String POLICY_ID1 = "policyId1";
  private static final String POLICY_ID2 = "policyId2";
  private static final String POLICY_ID3 = "policyId3";
  private static final int ORDER_POLICY1 = 1;
  private static final int ORDER_POLICY2 = 2;
  private static final int ORDER_POLICY3 = 3;

  private final PolicyInstanceProviderFactory policyInstanceProviderFactory = mock(PolicyInstanceProviderFactory.class);
  private final PolicyTemplateFactory policyTemplateFactory = mock(PolicyTemplateFactory.class);
  private final MuleApplicationPolicyProvider policyProvider =
      new MuleApplicationPolicyProvider(policyTemplateFactory, policyInstanceProviderFactory);
  private final Application application = mock(Application.class);
  private final PolicyPointcut pointcut = mock(PolicyPointcut.class);
  private final PolicyParametrization parametrization1 =
      new PolicyParametrization(POLICY_ID1, pointcut, ORDER_POLICY1, emptyMap(), mock(File.class), emptyList());
  private final PolicyParametrization parametrization2 =
      new PolicyParametrization(POLICY_ID2, pointcut, ORDER_POLICY2, emptyMap(), mock(File.class), emptyList());
  private final PolicyParametrization parametrization3 =
      new PolicyParametrization(POLICY_ID3, pointcut, ORDER_POLICY3, emptyMap(), mock(File.class), emptyList());
  private final PolicyTemplateDescriptor policyTemplateDescriptor1 = new PolicyTemplateDescriptor(POLICY_NAME1);
  private final PolicyTemplateDescriptor policyTemplateDescriptor2 = new PolicyTemplateDescriptor(POLICY_NAME2);
  private final PolicyPointcutParameters policyPointcutParameters = mock(PolicyPointcutParameters.class);
  private final ApplicationPolicyInstance applicationPolicyInstance1 = mock(ApplicationPolicyInstance.class);
  private final ApplicationPolicyInstance applicationPolicyInstance2 = mock(ApplicationPolicyInstance.class);
  private final ApplicationPolicyInstance applicationPolicyInstance3 = mock(ApplicationPolicyInstance.class);
  private final Policy policy1 = mock(Policy.class, POLICY_ID1);
  private final Policy policy2 = mock(Policy.class, POLICY_ID2);

  @Rule
  public ExpectedException expectedException = none();

  private PolicyTemplate policyTemplate1 = mock(PolicyTemplate.class);
  private PolicyTemplate policyTemplate2 = mock(PolicyTemplate.class);
  private RegionClassLoader regionClassLoader = mock(RegionClassLoader.class);
  private ArtifactClassLoader policyClassLoader1 = mock(ArtifactClassLoader.class);
  private ArtifactClassLoader policyClassLoader2 = mock(ArtifactClassLoader.class);

  @Before
  public void setUp() throws Exception {
    policyProvider.setApplication(application);
    when(application.getRegionClassLoader()).thenReturn(regionClassLoader);

    policyClassLoader1 = null;
    when(policyTemplate1.getArtifactClassLoader()).thenReturn(policyClassLoader1);
    when(policyTemplate2.getArtifactClassLoader()).thenReturn(policyClassLoader2);

    when(policyTemplateFactory.createArtifact(application, policyTemplateDescriptor1)).thenReturn(policyTemplate1);
    when(policyTemplateFactory.createArtifact(application, policyTemplateDescriptor2)).thenReturn(policyTemplate2);
    when(applicationPolicyInstance1.getPointcut()).thenReturn(pointcut);
    when(applicationPolicyInstance1.getOrder()).thenReturn(ORDER_POLICY1);
    when(applicationPolicyInstance1.getOperationPolicy()).thenReturn(of(policy1));
    when(applicationPolicyInstance1.getSourcePolicy()).thenReturn(of(policy1));
    when(applicationPolicyInstance1.getPolicyTemplate()).thenReturn(policyTemplate1);

    when(applicationPolicyInstance2.getPointcut()).thenReturn(pointcut);
    when(applicationPolicyInstance2.getOrder()).thenReturn(ORDER_POLICY2);
    when(applicationPolicyInstance2.getOperationPolicy()).thenReturn(of(policy2));
    when(applicationPolicyInstance2.getSourcePolicy()).thenReturn(of(policy2));
    when(applicationPolicyInstance2.getPolicyTemplate()).thenReturn(policyTemplate1);

    when(applicationPolicyInstance3.getPointcut()).thenReturn(pointcut);
    when(applicationPolicyInstance3.getOrder()).thenReturn(ORDER_POLICY3);
    when(applicationPolicyInstance3.getOperationPolicy()).thenReturn(empty());
    when(applicationPolicyInstance3.getSourcePolicy()).thenReturn(empty());
    when(applicationPolicyInstance3.getPolicyTemplate()).thenReturn(policyTemplate2);

    when(policyInstanceProviderFactory.create(application, policyTemplate1, parametrization1)).thenReturn(
                                                                                                          applicationPolicyInstance1);
    when(policyInstanceProviderFactory.create(application, policyTemplate1, parametrization2)).thenReturn(
                                                                                                          applicationPolicyInstance2);
    when(policyInstanceProviderFactory.create(application, policyTemplate2, parametrization3)).thenReturn(
                                                                                                          applicationPolicyInstance3);

    policyTemplateDescriptor1.setBundleDescriptor(new BundleDescriptor.Builder().setArtifactId(POLICY_NAME1).setGroupId("test")
        .setVersion("1.0").build());
    policyTemplateDescriptor2.setBundleDescriptor(new BundleDescriptor.Builder().setArtifactId(POLICY_NAME2).setGroupId("test")
        .setVersion("2.0").build());

    when(policyTemplate1.getDescriptor()).thenReturn(policyTemplateDescriptor1);
    when(policyTemplate2.getDescriptor()).thenReturn(policyTemplateDescriptor2);
  }

  @Test
  public void addsOperationPolicy() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(1));
    assertThat(parameterizedPolicies.get(0), is(policy1));
  }

  @Test
  public void addsOperationPolicyWithPointCut() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false);

    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(0));
  }

  @Test
  public void addsOperationPoliciesInOrder() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(2));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
  }

  @Test
  public void addsOperationPoliciesDisordered() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization2);
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(2));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
  }

  @Test
  public void addsOperationPoliciesWithAlwaysAcceptingPointCut() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(2));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
  }

  @Test
  public void addsOperationPoliciesWithAlwaysRejectingPointCut() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false).thenReturn(false);
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(0));
  }

  @Test
  public void addsOperationPoliciesWithPointCutAcceptingThenRejecting() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(false);

    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(1));
    assertThat(parameterizedPolicies.get(0), is(policy1));
  }

  @Test
  public void addsOperationPoliciesWithPointCutRejectingThenAccepting() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(1));
    assertThat(parameterizedPolicies.get(0), is(policy2));
  }

  @Test
  public void addsSourcePolicy() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(1));
    assertThat(parameterizedPolicies.get(0), is(policy1));
  }

  @Test
  public void addsSourcePolicyWithPointCut() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false);

    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(0));
  }

  @Test
  public void addsSourcePoliciesInOrder() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(2));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
  }

  @Test
  public void addsSourcePoliciesDisordered() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization2);
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(2));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
  }

  @Test
  public void addsSourcePoliciesWithAlwaysAcceptingPointCut() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(2));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
  }

  @Test
  public void addsSourcePoliciesWithAlwaysRejectingPointCut() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false).thenReturn(false);
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(0));
  }

  @Test
  public void addsSourcePoliciesWithPointCutAcceptingThenRejecting() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(false);

    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(1));
    assertThat(parameterizedPolicies.get(0), is(policy1));
  }

  @Test
  public void addsSourcePoliciesWithPointCutRejectingThenAccepting() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(1));
    assertThat(parameterizedPolicies.get(0), is(policy2));
  }

  @Test
  public void reusesPolicyTemplates() throws Exception {
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization2);

    verify(policyTemplateFactory).createArtifact(application, policyTemplateDescriptor1);
  }

  @Test
  public void maintainsPolicyTemplatesWhileUsed() throws Exception {
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization2);

    assertThat(policyProvider.removePolicy(parametrization1.getId()), is(true));
    verify(policyTemplate1, never()).dispose();
  }

  @Test
  public void disposesPolicyTemplatesWhenNotUsedAnymore() throws Exception {
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization2);

    assertThat(policyProvider.removePolicy(parametrization1.getId()), is(true));
    assertThat(policyProvider.removePolicy(parametrization2.getId()), is(true));

    verify(policyTemplate1).dispose();
    verify(regionClassLoader).removeClassLoader(policyClassLoader1);
  }

  @Test
  public void disposesPolicyTemplatesWhenNotUsedAnymoreMultipleTemplates() throws Exception {
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor2, parametrization3);

    assertThat(policyProvider.removePolicy(parametrization3.getId()), is(true));

    verify(policyTemplate2).dispose();
    verify(regionClassLoader).removeClassLoader(policyClassLoader2);
  }

  @Test
  public void detectsDuplicatePolicyId() throws Exception {
    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);

    expectedException.expect(PolicyRegistrationException.class);
    expectedException.expectMessage(createPolicyRegistrationError(POLICY_ID1));
    expectedException.expectCause(isA(IllegalArgumentException.class));

    policyProvider.addPolicy(policyTemplateDescriptor1, parametrization1);
  }

}
