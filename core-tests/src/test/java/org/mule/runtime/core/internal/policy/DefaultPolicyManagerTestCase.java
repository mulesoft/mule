/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.core.api.policy.PolicyProvider;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.EventInternalContext;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import io.qameta.allure.Issue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;

@RunWith(Parameterized.class)
public class DefaultPolicyManagerTestCase extends AbstractMuleContextTestCase {

  private static final int GC_POLLING_TIMEOUT = 10000;

  private PolicyProvider policyProvider;
  private ArgumentCaptor<Runnable> policiesChangeCallbackCaptor;
  private DefaultPolicyManager policyManager;

  private Component flowOne;
  private Component flowTwo;
  private Component testOperationOne;
  private Component testOperationTwo;
  private Component differentTestOperation;

  private final boolean featureFlagsEnabled;

  public DefaultPolicyManagerTestCase(boolean featureFlagsEnabled) {
    this.featureFlagsEnabled = featureFlagsEnabled;
  }

  @Parameterized.Parameters(name = "Apply Feature flags: {0}")
  public static Collection<Boolean> data() {
    return asList(
                  true,
                  false);
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    policyProvider = mock(PolicyProvider.class, RETURNS_DEEP_STUBS);
    return singletonMap("_policyProvider", policyProvider);
  }

  @Before
  public void before() throws InitialisationException {
    // Policies engine setup
    policyManager = new DefaultPolicyManager();
    initialiseIfNeeded(policyManager, muleContext);
    policiesChangeCallbackCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(policyProvider).onPoliciesChanged(policiesChangeCallbackCaptor.capture());
    when(policyProvider.isSourcePoliciesAvailable()).thenReturn(true);
    when(policyProvider.isOperationPoliciesAvailable()).thenReturn(true);
    // Flows setup
    flowOne = mock(Component.class);
    when(flowOne.getLocation()).thenReturn(fromSingleComponent("flow1"));
    when(flowOne.getRootContainerLocation()).thenReturn(builderFromStringRepresentation("flow1").build());
    flowTwo = mock(Component.class);
    when(flowTwo.getLocation()).thenReturn(fromSingleComponent("flow2"));
    when(flowTwo.getRootContainerLocation()).thenReturn(builderFromStringRepresentation("flow2").build());
    // Flow operations setup
    TypedComponentIdentifier testOperationComponentIdentifier = TypedComponentIdentifier.builder()
        .type(OPERATION)
        .identifier(ComponentIdentifier
            .buildFromStringRepresentation("test:operation"))
        .build();

    testOperationOne = mock(Component.class);
    ComponentLocation componentLocationOfTestOperationOne = mock(ComponentLocation.class);
    when(componentLocationOfTestOperationOne.getLocation()).thenReturn("flow/processors/1");
    when(componentLocationOfTestOperationOne.getComponentIdentifier()).thenReturn(testOperationComponentIdentifier);
    when(testOperationOne.getLocation()).thenReturn(componentLocationOfTestOperationOne);

    testOperationTwo = mock(Component.class);
    ComponentLocation componentLocationOfTestOperationTwo = mock(ComponentLocation.class);
    when(componentLocationOfTestOperationTwo.getLocation()).thenReturn("flow/processors/2");
    when(componentLocationOfTestOperationTwo.getComponentIdentifier()).thenReturn(testOperationComponentIdentifier);
    when(testOperationTwo.getLocation()).thenReturn(componentLocationOfTestOperationTwo);

    TypedComponentIdentifier differentTestOperationComponentIdentifier = TypedComponentIdentifier.builder()
        .type(OPERATION)
        .identifier(ComponentIdentifier
            .buildFromStringRepresentation("test:differentOperation"))
        .build();
    differentTestOperation = mock(Component.class);
    ComponentLocation componentLocationOfDifferentTestOperation = mock(ComponentLocation.class);
    when(componentLocationOfDifferentTestOperation.getLocation()).thenReturn("flow/processors/2");
    when(componentLocationOfDifferentTestOperation.getComponentIdentifier())
        .thenReturn(differentTestOperationComponentIdentifier);
    when(differentTestOperation.getLocation()).thenReturn(componentLocationOfDifferentTestOperation);
  }

  @After
  public void after() {
    policyManager.dispose();
  }

  @Test
  public void sourceNoPoliciesPresent() {
    when(policyProvider.isSourcePoliciesAvailable()).thenReturn(false);
    when(policyProvider.findSourceParameterizedPolicies(any())).thenReturn(emptyList());
    policiesChangeCallbackCaptor.getValue().run();

    final SourcePolicy policy1 = policyManager.createSourcePolicyInstance(flowOne, mock(CoreEvent.class), ePub -> ePub,
                                                                          mock(MessageSourceResponseParametersProcessor.class));
    final SourcePolicy policy2 = policyManager.createSourcePolicyInstance(flowTwo, mock(CoreEvent.class), ePub -> ePub,
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
    policiesChangeCallbackCaptor.getValue().run();

    final InternalEvent event1 = mock(InternalEvent.class);

    SourcePolicyContext ctx = mock(SourcePolicyContext.class);
    when(ctx.getPointcutParameters()).thenReturn(policyParams1);
    when(event1.getSourcePolicyContext()).thenReturn((EventInternalContext) ctx);

    final InternalEvent event2 = mock(InternalEvent.class);
    ctx = mock(SourcePolicyContext.class);
    when(ctx.getPointcutParameters()).thenReturn(policyParams2);
    when(event2.getSourcePolicyContext()).thenReturn((EventInternalContext) ctx);

    final SourcePolicy policy1 = policyManager.createSourcePolicyInstance(flowOne, event1, ePub -> ePub,
                                                                          mock(MessageSourceResponseParametersProcessor.class));
    final SourcePolicy policy2 = policyManager.createSourcePolicyInstance(flowOne, event2, ePub -> ePub,
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
    policiesChangeCallbackCaptor.getValue().run();

    final InternalEvent event = mock(InternalEvent.class);
    SourcePolicyContext ctx = mock(SourcePolicyContext.class);
    when(event.getSourcePolicyContext()).thenReturn((EventInternalContext) ctx);
    when(ctx.getPointcutParameters()).thenReturn(policyParams);

    final SourcePolicy policy1 = policyManager.createSourcePolicyInstance(flowOne, event, ePub -> ePub,
                                                                          mock(MessageSourceResponseParametersProcessor.class));
    final SourcePolicy policy2 = policyManager.createSourcePolicyInstance(flowTwo, event, ePub -> ePub,
                                                                          mock(MessageSourceResponseParametersProcessor.class));

    assertThat(policy1, instanceOf(CompositeSourcePolicy.class));
    assertThat(policy2, instanceOf(CompositeSourcePolicy.class));

    assertThat(policy1, not(policy2));
  }

  @Test
  public void operationNoPoliciesPresent() {
    when(policyProvider.isOperationPoliciesAvailable()).thenReturn(false);
    when(policyProvider.findOperationParameterizedPolicies(any())).thenReturn(emptyList());
    policiesChangeCallbackCaptor.getValue().run();

    final OperationPolicy policy1 = policyManager.createOperationPolicy(testOperationOne, mock(CoreEvent.class),
                                                                        mock(OperationParametersProcessor.class));
    final OperationPolicy policy2 = policyManager.createOperationPolicy(testOperationTwo, mock(CoreEvent.class),
                                                                        mock(OperationParametersProcessor.class));

    assertThat(policy1, not(instanceOf(CompositeOperationPolicy.class)));
    assertThat(policy2, not(instanceOf(CompositeOperationPolicy.class)));

    assertThat(policy1, sameInstance(policy2));
  }

  @Test
  @Issue("W-10620059")
  public void samePolicyWithSameParametersOnInstancesOfTheSameOperation() {
    Policy policy = mockPolicy();

    when(policyProvider.findOperationParameterizedPolicies(any(PolicyPointcutParameters.class)))
        .thenReturn(asList(policy));
    policiesChangeCallbackCaptor.getValue().run();

    final OperationPolicy operationPolicy1 = policyManager.createOperationPolicy(testOperationOne, mock(InternalEvent.class),
                                                                                 mock(OperationParametersProcessor.class));
    final OperationPolicy operationPolicy2 = policyManager.createOperationPolicy(testOperationTwo, mock(InternalEvent.class),
                                                                                 mock(OperationParametersProcessor.class));

    assertThat(operationPolicy1, instanceOf(CompositeOperationPolicy.class));
    assertThat(operationPolicy2, instanceOf(CompositeOperationPolicy.class));

    assertThat(operationPolicy1, sameInstance(operationPolicy2));
  }

  @Test
  @Issue("W-10620059")
  public void differentPoliciesWithDifferentParametersOnSameOperationInstance() {
    Policy policy1 = mockPolicy();
    Policy policy2 = mockPolicy();

    when(policyProvider.findOperationParameterizedPolicies(any(PolicyPointcutParameters.class)))
        .thenReturn(asList(policy1))
        .thenReturn(asList(policy2));
    policiesChangeCallbackCaptor.getValue().run();

    final PolicyPointcutParameters sourcePolicyParams1 = mock(PolicyPointcutParameters.class);
    final PolicyPointcutParameters sourcePolicyParams2 = mock(PolicyPointcutParameters.class);

    final InternalEvent event1 = mock(InternalEvent.class);
    SourcePolicyContext ctx = mock(SourcePolicyContext.class);
    when(event1.getSourcePolicyContext()).thenReturn((EventInternalContext) ctx);
    when(ctx.getPointcutParameters()).thenReturn(sourcePolicyParams1);

    final InternalEvent event2 = mock(InternalEvent.class);
    ctx = mock(SourcePolicyContext.class);
    when(event2.getSourcePolicyContext()).thenReturn((EventInternalContext) ctx);
    when(ctx.getPointcutParameters()).thenReturn(sourcePolicyParams2);

    final OperationPolicy operationPolicy1 = policyManager.createOperationPolicy(testOperationOne, event1,
                                                                                 mock(OperationParametersProcessor.class));
    final OperationPolicy operationPolicy2 = policyManager.createOperationPolicy(testOperationOne, event2,
                                                                                 mock(OperationParametersProcessor.class));

    assertThat(operationPolicy1, instanceOf(CompositeOperationPolicy.class));
    assertThat(operationPolicy2, instanceOf(CompositeOperationPolicy.class));

    assertThat(operationPolicy1, not(operationPolicy2));
  }

  @Test
  @Issue("W-10620059")
  public void samePolicyWithSameParametersOnInstancesOfDifferentOperations() {
    Policy policy = mockPolicy();

    when(policyProvider.findOperationParameterizedPolicies(any(PolicyPointcutParameters.class)))
        .thenReturn(asList(policy));
    policiesChangeCallbackCaptor.getValue().run();

    final OperationPolicy operationPolicy1 = policyManager.createOperationPolicy(testOperationOne, mock(InternalEvent.class),
                                                                                 mock(OperationParametersProcessor.class));
    final OperationPolicy operationPolicy2 =
        policyManager.createOperationPolicy(differentTestOperation, mock(InternalEvent.class),
                                            mock(OperationParametersProcessor.class));

    assertThat(operationPolicy1, instanceOf(CompositeOperationPolicy.class));
    assertThat(operationPolicy2, instanceOf(CompositeOperationPolicy.class));

    assertThat(operationPolicy1, not(operationPolicy2));
  }

  @Test
  public void sourcePolicyAcceptsWhileAvailable() throws MuleException {
    startIfNeeded(policyManager);

    AtomicBoolean sourcePolicydisposed = new AtomicBoolean(false);

    policyManager.setCompositePolicyFactory(new CompositePolicyFactory() {

      @Override
      public SourcePolicy createSourcePolicy(List<Policy> innerKey, ReactiveProcessor flowExecutionProcessor,
                                             Optional<SourcePolicyParametersTransformer> lookupSourceParametersTransformer,
                                             SourcePolicyProcessorFactory sourcePolicyProcessorFactory,
                                             Function<MessagingException, MessagingException> resolver) {
        final SourcePolicy sourcePolicy =
            super.createSourcePolicy(innerKey, flowExecutionProcessor, lookupSourceParametersTransformer,
                                     sourcePolicyProcessorFactory, resolver);
        final Disposable deferredDispose = ((DeferredDisposable) sourcePolicy).deferredDispose();

        return new DisposeListenerSourcePolicy(sourcePolicy, () -> {
          deferredDispose.dispose();
          sourcePolicydisposed.set(true);
        });
      }
    });

    final Policy policy = mockPolicy();

    when(policyProvider.findSourceParameterizedPolicies(any())).thenReturn(asList(policy));

    policiesChangeCallbackCaptor.getValue().run();

    final InternalEvent event = mock(InternalEvent.class);

    SourcePolicyContext ctx = mock(SourcePolicyContext.class);
    when(ctx.getPointcutParameters()).thenReturn(mock(PolicyPointcutParameters.class));
    when(event.getSourcePolicyContext()).thenReturn((EventInternalContext) ctx);

    SourcePolicy policyInstance =
        policyManager.createSourcePolicyInstance(flowOne, event, ePub -> ePub,
                                                 mock(MessageSourceResponseParametersProcessor.class));

    // Clear the caches of the policyManager...
    policiesChangeCallbackCaptor.getValue().run();

    System.gc();
    // Verify that the previously obtained policyInstance is still good
    assertThat(sourcePolicydisposed.get(), is(false));

    policyInstance = null;

    // Verify that dispose is called when the policy is no longer being used
    new PollingProber(GC_POLLING_TIMEOUT, DEFAULT_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      System.gc();
      return sourcePolicydisposed.get();
    }));

    stopIfNeeded(policyManager);
  }

  @Test
  public void operationPolicyAcceptsWhileAvailable() throws MuleException {
    startIfNeeded(policyManager);

    AtomicBoolean operationPolicydisposed = new AtomicBoolean(false);

    policyManager.setCompositePolicyFactory(new CompositePolicyFactory() {

      @Override
      public OperationPolicy createOperationPolicy(Component operation, List<Policy> innerKey,
                                                   Optional<OperationPolicyParametersTransformer> paramsTransformer,
                                                   OperationPolicyProcessorFactory operationPolicyProcessorFactory,
                                                   long shutdownTimeout, Scheduler scheduler,
                                                   FeatureFlaggingService featureFlaggingService) {
        final OperationPolicy operationPolicy =
            super.createOperationPolicy(operation, innerKey, paramsTransformer, operationPolicyProcessorFactory, shutdownTimeout,
                                        scheduler, feature -> featureFlagsEnabled);

        final Disposable deferredDispose = ((DeferredDisposable) operationPolicy).deferredDispose();

        return new DisposeListenerSourcePolicy(operationPolicy, () -> {
          deferredDispose.dispose();
          scheduler.stop();
          operationPolicydisposed.set(true);
        });
      }
    });

    final Policy policy = mockPolicy();

    when(policyProvider.findOperationParameterizedPolicies(any())).thenReturn(asList(policy));

    policiesChangeCallbackCaptor.getValue().run();

    final InternalEvent event = mock(InternalEvent.class);

    OperationPolicyContext ctx = mock(OperationPolicyContext.class);
    when(event.getOperationPolicyContext()).thenReturn((EventInternalContext) ctx);

    OperationPolicy policyInstance =
        policyManager.createOperationPolicy(flowOne, event, mock(OperationParametersProcessor.class));

    // Clear the caches of the policyManager...
    policiesChangeCallbackCaptor.getValue().run();

    System.gc();
    // Verify that the previously obtained policyInstance is still good
    assertThat(operationPolicydisposed.get(), is(false));

    policyInstance = null;

    // Verify that dispose is called when the policy is no longer being used
    new PollingProber(GC_POLLING_TIMEOUT, DEFAULT_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      System.gc();
      return operationPolicydisposed.get();
    }));

    stopIfNeeded(policyManager);
  }

  @Test
  public void cachesEvictedWhileLookingForPolicies() throws InterruptedException {
    final Policy policy = mock(Policy.class, RETURNS_DEEP_STUBS);
    when(policyProvider.isSourcePoliciesAvailable()).thenReturn(true);
    when(policyProvider.findSourceParameterizedPolicies(any())).thenReturn(asList(policy));
    policiesChangeCallbackCaptor.getValue().run();

    InternalEvent event = mock(InternalEvent.class);
    SourcePolicyContext ctx = mock(SourcePolicyContext.class);
    when(event.getSourcePolicyContext()).thenReturn((EventInternalContext) ctx);
    when(ctx.getPointcutParameters()).thenReturn(mock(PolicyPointcutParameters.class));

    CountDownLatch lookingForPoliciesLatch = new CountDownLatch(1);
    CountDownLatch cacheEvictedLatch = new CountDownLatch(1);

    when(policyProvider.findSourceParameterizedPolicies(any())).thenAnswer(invocation -> {
      lookingForPoliciesLatch.countDown();
      assertThat("Eviction should not possible while looking for policies",
                 cacheEvictedLatch.await(3, SECONDS), is(false));
      return asList(policy);
    });

    // While findSourceParameterizedPolicies is executed, policy is removed, therefore caches should be evicted
    new Thread(() -> {
      try {
        lookingForPoliciesLatch.await();
        when(policyProvider.isSourcePoliciesAvailable()).thenReturn(false);
        policiesChangeCallbackCaptor.getValue().run();
        cacheEvictedLatch.countDown();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }).start();

    final SourcePolicy policy1 = policyManager.createSourcePolicyInstance(flowOne, event, ePub -> ePub,
                                                                          mock(MessageSourceResponseParametersProcessor.class));
    cacheEvictedLatch.await();
    final SourcePolicy policy2 = policyManager.createSourcePolicyInstance(flowOne, event, ePub -> ePub,
                                                                          mock(MessageSourceResponseParametersProcessor.class));

    assertThat(policy1, instanceOf(CompositeSourcePolicy.class));
    assertThat(policy2, instanceOf(NoSourcePolicy.class));
  }

  @Test
  @Issue("MULE-18929")
  public void cachesEvictedDoesntIncreaseActivePoliciesCount() throws InterruptedException {
    final Policy policy = mock(Policy.class, RETURNS_DEEP_STUBS);
    when(policyProvider.isSourcePoliciesAvailable()).thenReturn(true);
    when(policyProvider.findSourceParameterizedPolicies(any())).thenReturn(asList(policy));
    policiesChangeCallbackCaptor.getValue().run();

    policyManager.setOuterCachesExpireTime(1, SECONDS);

    InternalEvent event = mock(InternalEvent.class);
    SourcePolicyContext ctx = mock(SourcePolicyContext.class);
    when(event.getSourcePolicyContext()).thenReturn((EventInternalContext) ctx);
    when(ctx.getPointcutParameters()).thenReturn(mock(PolicyPointcutParameters.class));

    when(policyProvider.findSourceParameterizedPolicies(any())).thenReturn(asList(policy));

    final SourcePolicy policy1 = policyManager.createSourcePolicyInstance(flowOne, event, ePub -> ePub,
                                                                          mock(MessageSourceResponseParametersProcessor.class));

    sleep(1500);

    final SourcePolicy policy2 = policyManager.createSourcePolicyInstance(flowOne, event, ePub -> ePub,
                                                                          mock(MessageSourceResponseParametersProcessor.class));

    assertThat(policy1, sameInstance(policy2));
    assertThat(policyManager.getActivePoliciesCount(), is(1));
  }

  private Policy mockPolicy() {
    PolicyChain policyChain = mock(PolicyChain.class, RETURNS_DEEP_STUBS);
    when(policyChain.apply(any()))
        .thenAnswer(inv -> inv.getArgument(0));
    when(policyChain.getProcessingStrategy().onPipeline(any()))
        .thenAnswer(inv -> inv.getArgument(0));

    Policy policy = mock(Policy.class, RETURNS_DEEP_STUBS);
    when(policy.getPolicyChain()).thenReturn(policyChain);

    return policy;
  }

  private static class DisposeListenerSourcePolicy implements SourcePolicy, OperationPolicy, Disposable, DeferredDisposable {

    private final Object reference;
    private final Disposable onDispose;

    public DisposeListenerSourcePolicy(Object reference, Disposable onDispose) {
      this.reference = reference;
      this.onDispose = onDispose;
    }

    @Override
    public void dispose() {
      onDispose.dispose();
    }

    @Override
    public Disposable deferredDispose() {
      return onDispose;
    }

    @Override
    public void process(CoreEvent sourceEvent, MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor,
                        CompletableCallback<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> callback) {
      // nothing to do
    }

    @Override
    public void process(CoreEvent operationEvent, OperationExecutionFunction operationExecutionFunction,
                        OperationParametersProcessor parametersProcessor, ComponentLocation componentLocation,
                        ExecutorCallback callback) {
      // nothing to do
    }
  }
}
