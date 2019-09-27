/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.internal.policy.PolicyPointcutParametersManager.POLICY_SOURCE_POINTCUT_PARAMETERS;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.core.api.policy.PolicyProvider;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

public class DefaultPolicyManagerTestCase extends AbstractMuleTestCase {

  private PolicyProvider policyProvider;
  private ArgumentCaptor<Runnable> policiesChangeCallbackCaptor;
  private DefaultPolicyManager policyManager;

  private Component flow1Component;
  private Component flow2Component;
  private Component operation1Component;
  private Component operation2Component;

  @Before
  public void before() throws InitialisationException {
    policyManager = new DefaultPolicyManager();

    policyProvider = mock(PolicyProvider.class, RETURNS_DEEP_STUBS);
    final Registry registry = mock(Registry.class);
    when(registry.lookupByType(PolicyProvider.class)).thenReturn(of(policyProvider));
    policyManager.setRegistry(registry);

    final MuleContext muleContext = mock(MuleContext.class);
    when(muleContext.getArtifactType()).thenReturn(ArtifactType.APP);
    ServerNotificationManager notificationManagerMock = mock(ServerNotificationManager.class);
    doNothing().when(notificationManagerMock).addListener(any());
    doReturn(notificationManagerMock).when(muleContext).getNotificationManager();
    policyManager.setMuleContext(muleContext);

    policyManager.initialise();

    policiesChangeCallbackCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(policyProvider).onPoliciesChanged(policiesChangeCallbackCaptor.capture());

    flow1Component = mock(Component.class);
    when(flow1Component.getLocation()).thenReturn(fromSingleComponent("flow1"));
    flow2Component = mock(Component.class);
    when(flow2Component.getLocation()).thenReturn(fromSingleComponent("flow2"));

    operation1Component = mock(Component.class);
    when(operation1Component.getLocation()).thenReturn(fromSingleComponent("flow/processors/1"));
    operation2Component = mock(Component.class);
    when(operation2Component.getLocation()).thenReturn(fromSingleComponent("flow/processors/2"));
  }

  @After
  public void after() {
    policyManager.dispose();
  }

  @Test
  public void sourceNoPoliciesPresent() {
    when(policyProvider.findSourceParameterizedPolicies(any())).thenReturn(emptyList());
    policiesChangeCallbackCaptor.getValue().run();

    final SourcePolicy policy1 = policyManager.createSourcePolicyInstance(flow1Component, mock(CoreEvent.class), ePub -> ePub,
                                                                          mock(MessageSourceResponseParametersProcessor.class));
    final SourcePolicy policy2 = policyManager.createSourcePolicyInstance(flow2Component, mock(CoreEvent.class), ePub -> ePub,
                                                                          mock(MessageSourceResponseParametersProcessor.class));

    assertThat(policy1, instanceOf(NoSourcePolicy.class));
    assertThat(policy2, instanceOf(NoSourcePolicy.class));

    assertThat(policy1, not(policy2));
  }

  @Test
  public void sourceSamePolicyForDifferentParams() {
    final PolicyPointcutParameters policyParams1 = mock(PolicyPointcutParameters.class);
    final PolicyPointcutParameters policyParams2 = mock(PolicyPointcutParameters.class);

    final Policy policy = mock(Policy.class, RETURNS_DEEP_STUBS);
    final PolicyChain policyChain = policy.getPolicyChain();
    when(policyChain.onChainError(any())).thenReturn(policyChain);

    when(policyProvider.findSourceParameterizedPolicies(policyParams1)).thenReturn(asList(policy));
    when(policyProvider.findSourceParameterizedPolicies(policyParams2)).thenReturn(asList(policy));
    when(policyProvider.isPoliciesAvailable()).thenReturn(true);
    policiesChangeCallbackCaptor.getValue().run();

    final InternalEvent event1 = mock(InternalEvent.class);
    when(event1.getInternalParameter(POLICY_SOURCE_POINTCUT_PARAMETERS)).thenReturn(policyParams1);

    final InternalEvent event2 = mock(InternalEvent.class);
    when(event2.getInternalParameter(POLICY_SOURCE_POINTCUT_PARAMETERS)).thenReturn(policyParams2);

    final SourcePolicy policy1 = policyManager.createSourcePolicyInstance(flow1Component, event1, ePub -> ePub,
                                                                          mock(MessageSourceResponseParametersProcessor.class));
    final SourcePolicy policy2 = policyManager.createSourcePolicyInstance(flow1Component, event2, ePub -> ePub,
                                                                          mock(MessageSourceResponseParametersProcessor.class));

    assertThat(policy1, instanceOf(CompositeSourcePolicy.class));
    assertThat(policy2, instanceOf(CompositeSourcePolicy.class));

    assertThat(policy1, sameInstance(policy2));
  }

  @Test
  public void sourceDifferentPolicyForDifferentFlowSameParams() {
    final PolicyPointcutParameters policyParams = mock(PolicyPointcutParameters.class);

    final Policy policy = mock(Policy.class, RETURNS_DEEP_STUBS);
    final PolicyChain policyChain = policy.getPolicyChain();
    when(policyChain.onChainError(any())).thenReturn(policyChain);

    when(policyProvider.findSourceParameterizedPolicies(policyParams)).thenReturn(asList(policy));
    when(policyProvider.isPoliciesAvailable()).thenReturn(true);
    policiesChangeCallbackCaptor.getValue().run();

    final InternalEvent event = mock(InternalEvent.class);
    when(event.getInternalParameter(POLICY_SOURCE_POINTCUT_PARAMETERS)).thenReturn(policyParams);

    final SourcePolicy policy1 = policyManager.createSourcePolicyInstance(flow1Component, event, ePub -> ePub,
                                                                          mock(MessageSourceResponseParametersProcessor.class));
    final SourcePolicy policy2 = policyManager.createSourcePolicyInstance(flow2Component, event, ePub -> ePub,
                                                                          mock(MessageSourceResponseParametersProcessor.class));

    assertThat(policy1, instanceOf(CompositeSourcePolicy.class));
    assertThat(policy2, instanceOf(CompositeSourcePolicy.class));

    assertThat(policy1, not(policy2));
  }

  @Test
  public void operationNoPoliciesPresent() {
    when(policyProvider.findOperationParameterizedPolicies(any())).thenReturn(emptyList());
    policiesChangeCallbackCaptor.getValue().run();

    final OperationPolicy policy1 = policyManager.createOperationPolicy(operation1Component, mock(CoreEvent.class),
                                                                        mock(OperationParametersProcessor.class));
    final OperationPolicy policy2 = policyManager.createOperationPolicy(operation2Component, mock(CoreEvent.class),
                                                                        mock(OperationParametersProcessor.class));

    assertThat(policy1, not(instanceOf(CompositeOperationPolicy.class)));
    assertThat(policy2, not(instanceOf(CompositeOperationPolicy.class)));

    assertThat(policy1, sameInstance(policy2));
  }

  @Test
  public void operationSamePolicyForSameParams() {
    Policy policy = mockPolicy();

    when(policyProvider.findOperationParameterizedPolicies(any(PolicyPointcutParameters.class)))
        .thenReturn(asList(policy));
    when(policyProvider.isPoliciesAvailable()).thenReturn(true);
    policiesChangeCallbackCaptor.getValue().run();

    final OperationPolicy operationPolicy1 = policyManager.createOperationPolicy(operation1Component, mock(InternalEvent.class),
                                                                                 mock(OperationParametersProcessor.class));
    final OperationPolicy operationPolicy2 = policyManager.createOperationPolicy(operation2Component, mock(InternalEvent.class),
                                                                                 mock(OperationParametersProcessor.class));

    assertThat(operationPolicy1, instanceOf(CompositeOperationPolicy.class));
    assertThat(operationPolicy2, instanceOf(CompositeOperationPolicy.class));

    assertThat(operationPolicy1, sameInstance(operationPolicy2));
  }

  @Test
  public void operationDifferentPolicyForDifferentParams() {
    Policy policy1 = mockPolicy();
    Policy policy2 = mockPolicy();

    when(policyProvider.findOperationParameterizedPolicies(any(PolicyPointcutParameters.class)))
        .thenReturn(asList(policy1))
        .thenReturn(asList(policy2));
    when(policyProvider.isPoliciesAvailable()).thenReturn(true);
    policiesChangeCallbackCaptor.getValue().run();

    final PolicyPointcutParameters sourcePolicyParams1 = mock(PolicyPointcutParameters.class);
    final PolicyPointcutParameters sourcePolicyParams2 = mock(PolicyPointcutParameters.class);

    final InternalEvent event1 = mock(InternalEvent.class);
    when(event1.getInternalParameter(POLICY_SOURCE_POINTCUT_PARAMETERS)).thenReturn(sourcePolicyParams1);
    final InternalEvent event2 = mock(InternalEvent.class);
    when(event2.getInternalParameter(POLICY_SOURCE_POINTCUT_PARAMETERS)).thenReturn(sourcePolicyParams2);

    final OperationPolicy operationPolicy1 = policyManager.createOperationPolicy(operation1Component, event1,
                                                                                 mock(OperationParametersProcessor.class));
    final OperationPolicy operationPolicy2 = policyManager.createOperationPolicy(operation1Component, event2,
                                                                                 mock(OperationParametersProcessor.class));

    assertThat(operationPolicy1, instanceOf(CompositeOperationPolicy.class));
    assertThat(operationPolicy2, instanceOf(CompositeOperationPolicy.class));

    assertThat(policy1, not(policy2));
  }

  @Test
  public void operationSamePolicyForDifferentOperationSameParams() {
    Policy policy = mockPolicy();
    when(policyProvider.findOperationParameterizedPolicies(any(PolicyPointcutParameters.class)))
        .thenReturn(asList(policy));
    when(policyProvider.isPoliciesAvailable()).thenReturn(true);
    policiesChangeCallbackCaptor.getValue().run();

    final OperationPolicy policy1 = policyManager.createOperationPolicy(operation1Component, mock(InternalEvent.class),
                                                                        mock(OperationParametersProcessor.class));
    final OperationPolicy policy2 = policyManager.createOperationPolicy(operation2Component, mock(InternalEvent.class),
                                                                        mock(OperationParametersProcessor.class));

    assertThat(policy1, instanceOf(CompositeOperationPolicy.class));
    assertThat(policy2, instanceOf(CompositeOperationPolicy.class));

    assertThat(policy1, sameInstance(policy2));
  }

  private Policy mockPolicy() {
    Policy policy = mock(Policy.class, RETURNS_DEEP_STUBS);
    when(policy.getPolicyChain().getProcessingStrategy().onPipeline(ArgumentMatchers.any()))
        .thenAnswer(inv -> inv.getArgument(0));

    return policy;
  }

}
