/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.application;

import static java.util.Collections.emptyMap;
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

import org.mule.runtime.core.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.core.api.policy.PolicyPointcut;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.policy.PolicyRegistrationException;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
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

  private static final String POLICY_NAME = "testPolicy";
  private static final String POLICY_ID1 = "policyId1";
  private static final String POLICY_ID2 = "policyId2";
  private static final int ORDER_POLICY1 = 1;
  private static final int ORDER_POLICY2 = 2;

  private final PolicyInstanceProviderFactory policyInstanceProviderFactory = mock(PolicyInstanceProviderFactory.class);
  private final PolicyTemplateFactory policyTemplateFactory = mock(PolicyTemplateFactory.class);
  private final MuleApplicationPolicyProvider policyProvider =
      new MuleApplicationPolicyProvider(policyTemplateFactory, policyInstanceProviderFactory);
  private final Application application = mock(Application.class);
  private final PolicyPointcut pointcut = mock(PolicyPointcut.class);
  private final PolicyParametrization parametrization1 =
      new PolicyParametrization(POLICY_ID1, pointcut, ORDER_POLICY1, emptyMap(), mock(File.class));
  private final PolicyParametrization parametrization2 =
      new PolicyParametrization(POLICY_ID2, pointcut, ORDER_POLICY2, emptyMap(), mock(File.class));
  private final PolicyTemplateDescriptor policyTemplateDescriptor = new PolicyTemplateDescriptor(POLICY_NAME);
  private final PolicyPointcutParameters policyPointcutParameters = mock(PolicyPointcutParameters.class);
  private PolicyTemplate policyTemplate = mock(PolicyTemplate.class);
  private final ApplicationPolicyInstance applicationPolicyInstance1 = mock(ApplicationPolicyInstance.class);
  private final ApplicationPolicyInstance applicationPolicyInstance2 = mock(ApplicationPolicyInstance.class);
  private final Policy policy1 = mock(Policy.class, POLICY_ID1);
  private final Policy policy2 = mock(Policy.class, POLICY_ID2);
  private RegionClassLoader regionClassLoader = mock(RegionClassLoader.class);
  private ArtifactClassLoader policyClassLoader = mock(ArtifactClassLoader.class);

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void setUp() throws Exception {
    policyProvider.setApplication(application);
    when(application.getRegionClassLoader()).thenReturn(regionClassLoader);

    policyClassLoader = null;
    when(policyTemplate.getArtifactClassLoader()).thenReturn(policyClassLoader);

    when(policyTemplateFactory.createArtifact(application, policyTemplateDescriptor)).thenReturn(policyTemplate);
    when(applicationPolicyInstance1.getPointcut()).thenReturn(pointcut);
    when(applicationPolicyInstance1.getOrder()).thenReturn(ORDER_POLICY1);
    when(applicationPolicyInstance1.getOperationPolicy()).thenReturn(of(policy1));
    when(applicationPolicyInstance1.getSourcePolicy()).thenReturn(of(policy1));

    when(applicationPolicyInstance2.getPointcut()).thenReturn(pointcut);
    when(applicationPolicyInstance2.getOrder()).thenReturn(ORDER_POLICY2);
    when(applicationPolicyInstance2.getOperationPolicy()).thenReturn(of(policy2));
    when(applicationPolicyInstance2.getSourcePolicy()).thenReturn(of(policy2));

    when(policyInstanceProviderFactory.create(application, policyTemplate, parametrization1)).thenReturn(
                                                                                                         applicationPolicyInstance1);
    when(policyInstanceProviderFactory.create(application, policyTemplate, parametrization2)).thenReturn(
                                                                                                         applicationPolicyInstance2);

    policyTemplateDescriptor.setBundleDescriptor(new BundleDescriptor.Builder().setArtifactId(POLICY_NAME).setGroupId("test")
        .setVersion("1.0").build());
    when(policyTemplate.getDescriptor()).thenReturn(policyTemplateDescriptor);
  }

  @Test
  public void addsOperationPolicy() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(1));
    assertThat(parameterizedPolicies.get(0), is(policy1));
  }

  @Test
  public void addsOperationPolicyWithPointCut() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(0));
  }

  @Test
  public void addsOperationPoliciesInOrder() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(2));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
  }

  @Test
  public void addsOperationPoliciesDisordered() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization2);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(2));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
  }

  @Test
  public void addsOperationPoliciesWithAlwaysAcceptingPointCut() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(2));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
  }

  @Test
  public void addsOperationPoliciesWithAlwaysRejectingPointCut() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false).thenReturn(false);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(0));
  }

  @Test
  public void addsOperationPoliciesWithPointCutAcceptingThenRejecting() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(false);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(1));
    assertThat(parameterizedPolicies.get(0), is(policy1));
  }

  @Test
  public void addsOperationPoliciesWithPointCutRejectingThenAccepting() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(1));
    assertThat(parameterizedPolicies.get(0), is(policy2));
  }

  @Test
  public void addsSourcePolicy() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(1));
    assertThat(parameterizedPolicies.get(0), is(policy1));
  }

  @Test
  public void addsSourcePolicyWithPointCut() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(0));
  }

  @Test
  public void addsSourcePoliciesInOrder() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(2));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
  }

  @Test
  public void addsSourcePoliciesDisordered() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization2);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(2));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
  }

  @Test
  public void addsSourcePoliciesWithAlwaysAcceptingPointCut() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(2));
    assertThat(parameterizedPolicies.get(0), is(policy1));
    assertThat(parameterizedPolicies.get(1), is(policy2));
  }

  @Test
  public void addsSourcePoliciesWithAlwaysRejectingPointCut() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false).thenReturn(false);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(0));
  }

  @Test
  public void addsSourcePoliciesWithPointCutAcceptingThenRejecting() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(true).thenReturn(false);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(1));
    assertThat(parameterizedPolicies.get(0), is(policy1));
  }

  @Test
  public void addsSourcePoliciesWithPointCutRejectingThenAccepting() throws Exception {
    when(pointcut.matches(policyPointcutParameters)).thenReturn(false).thenReturn(true);

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization2);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(policyPointcutParameters);

    assertThat(parameterizedPolicies.size(), equalTo(1));
    assertThat(parameterizedPolicies.get(0), is(policy2));
  }

  @Test
  public void reusesPolicyTemplates() throws Exception {
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization2);

    verify(policyTemplateFactory).createArtifact(application, policyTemplateDescriptor);
  }

  @Test
  public void maintainsPolicyTemplatesWhileUsed() throws Exception {
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization2);

    assertThat(policyProvider.removePolicy(parametrization1.getId()), is(true));
    verify(policyTemplate, never()).dispose();
  }

  @Test
  public void disposesPolicyTemplatesWhenNotUsedAnymore() throws Exception {
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization2);

    assertThat(policyProvider.removePolicy(parametrization1.getId()), is(true));
    assertThat(policyProvider.removePolicy(parametrization2.getId()), is(true));

    verify(policyTemplate).dispose();
    verify(regionClassLoader).removeClassLoader(policyClassLoader);
  }

  @Test
  public void detectsDuplicatePolicyId() throws Exception {
    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);

    expectedException.expect(PolicyRegistrationException.class);
    expectedException.expectMessage(createPolicyRegistrationError(POLICY_ID1));
    expectedException.expectCause(isA(IllegalArgumentException.class));

    policyProvider.addPolicy(policyTemplateDescriptor, parametrization1);
  }
}
