/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;

import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.core.api.policy.PolicyProvider;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.util.CaseInsensitiveHashMap;
import org.mule.runtime.core.internal.event.InternalEvent;
import org.mule.runtime.core.internal.message.EventInternalContext;
import org.mule.runtime.core.internal.processor.strategy.DirectProcessingStrategyFactory;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.mockito.ArgumentCaptor;

import io.qameta.allure.Issue;

import reactor.core.publisher.Flux;

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
    clearPolicyManagerCaches();

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
    clearPolicyManagerCaches();

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
    clearPolicyManagerCaches();

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
  public void sourceSamePolicyForDifferentFlowSameParams() {
    final PolicyPointcutParameters policyParams = mock(PolicyPointcutParameters.class);

    final Policy policy = mock(Policy.class, RETURNS_DEEP_STUBS);
    final PolicyChain policyChain = policy.getPolicyChain();
    when(policyChain.onChainError(any())).thenReturn(policyChain);

    when(policyProvider.findSourceParameterizedPolicies(policyParams)).thenReturn(asList(policy));
    clearPolicyManagerCaches();

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
    clearPolicyManagerCaches();

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
    Policy policy = stubPolicy("PolicyStub");

    when(policyProvider.findOperationParameterizedPolicies(any(PolicyPointcutParameters.class)))
        .thenReturn(asList(policy));
    clearPolicyManagerCaches();

    final OperationPolicy operationPolicy1 = policyManager.createOperationPolicy(testOperationOne, mock(InternalEvent.class),
                                                                                 mock(OperationParametersProcessor.class));
    final OperationPolicy operationPolicy2 = policyManager.createOperationPolicy(testOperationTwo, mock(InternalEvent.class),
                                                                                 mock(OperationParametersProcessor.class));

    assertThat(operationPolicy1, instanceOf(CompositeOperationPolicy.class));
    assertThat(operationPolicy2, instanceOf(CompositeOperationPolicy.class));

    assertThat(operationPolicy1, not(operationPolicy2));
  }

  @Test
  @Issue("W-10620059")
  public void differentPoliciesWithDifferentParametersOnSameOperationInstance() {
    Policy policy1 = stubPolicy("PolicyStub");
    Policy policy2 = stubPolicy("PolicyStub2");

    when(policyProvider.findOperationParameterizedPolicies(any(PolicyPointcutParameters.class)))
        .thenReturn(asList(policy1))
        .thenReturn(asList(policy2));
    clearPolicyManagerCaches();

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
    Policy policy = stubPolicy("PolicyStub");

    when(policyProvider.findOperationParameterizedPolicies(any(PolicyPointcutParameters.class)))
        .thenReturn(asList(policy));
    clearPolicyManagerCaches();

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
  public void sourcePolicyDisposeWithNoInflightEvents() throws MuleException {
    startIfNeeded(policyManager);
    clearPolicyManagerCaches();

    // Boolean that will be true if the policy gets disposed.
    AtomicBoolean isSourcePolicyDisposed = new AtomicBoolean(false);
    trackPolicyDisposal(isSourcePolicyDisposed);

    // Creating a policy through the policy manager.
    SourcePolicy sourcePolicy = createSourcePolicy();

    // Clearing the policy manager caches should not dispose the policy since there are still references to it.
    clearPolicyManagerCaches();
    System.gc();
    assertThat(isSourcePolicyDisposed.get(), is(false));

    // Dereference of the policy should trigger its disposal.
    sourcePolicy = null;
    System.gc();
    verifyPolicyDisposal(isSourcePolicyDisposed);

    stopIfNeeded(policyManager);
  }

  private InternalEvent getEventMock() {
    InternalEvent event = mock(InternalEvent.class);
    SourcePolicyContext ctx = mock(SourcePolicyContext.class);
    when(ctx.getPointcutParameters()).thenReturn(mock(PolicyPointcutParameters.class));
    when(event.getSourcePolicyContext()).thenReturn((EventInternalContext) ctx);
    return event;
  }

  @Test
  @Issue("W-10917220")
  public void sourcePolicyIsActiveWhileEventsAreInflight() throws MuleException, InterruptedException {
    startIfNeeded(policyManager);
    clearPolicyManagerCaches();

    // Creating a policy through the policy manager.
    Policy policy = stubPolicy("PolicyStub");
    SourcePolicy sourcePolicy = createSourcePolicy(policy);

    // We will track the policy in order to know if it has been finalized (this implies that it will be later disposed by the
    // policy manager).
    ReferenceQueue<SourcePolicy> sourcePolicyReferenceQueue = new ReferenceQueue<>();
    PhantomReference<SourcePolicy> sourcePolicyPhantomReference =
        new PhantomReference<>(sourcePolicy, sourcePolicyReferenceQueue);

    // An event and a source policy context are created in order to emulate an inflight event.
    InternalEvent event = mock(InternalEvent.class, RETURNS_DEEP_STUBS);
    PolicyPointcutParameters policyPointcutParameters = mock(PolicyPointcutParameters.class);
    SourcePolicyContext sourcePolicyContext = new SourcePolicyContext(policyPointcutParameters);
    when(event.getSourcePolicyContext()).thenReturn((EventInternalContext) sourcePolicyContext);
    when(event.getVariables()).thenReturn(mock(CaseInsensitiveHashMap.class));
    when(event.getParameters()).thenReturn(mock(CaseInsensitiveHashMap.class));

    // The event is sent through the policy, becoming an inflight one.
    sourcePolicy.process(event, mock(MessageSourceResponseParametersProcessor.class), mock(CompletableCallback.class));

    // Since there are inflight events, clearing the policy manager caches and de-referencing the policy should not trigger its
    // dispose. We check for the phantom reference because it implies later disposal by the manager (it is done asynchronously).
    clearPolicyManagerCaches();
    sourcePolicy = null;
    System.gc();
    assertThat(sourcePolicyReferenceQueue.remove(GC_POLLING_TIMEOUT), nullValue());
    verifyActivePolicies(1);

    // No more inflight events should trigger the policy disposal.
    // This is needed because of some mockito references.
    reset(event);
    event = null;
    sourcePolicyContext = null;
    System.gc();
    // We cannot use a wrapper of the policy to track its disposal as in other tests (see trackPolicyDisposal()).
    // Note that sourcePolicyDisposeWithNoInflightEvents() test is actually asserting it, so both tests are complementary to each
    // other.
    verifyActivePolicies(0);
    stopIfNeeded(policyManager);
  }

  private void clearPolicyManagerCaches() {
    policiesChangeCallbackCaptor.getValue().run();
  }

  private SourcePolicy createSourcePolicy() {
    return createSourcePolicy(stubPolicy("PolicyStub"));
  }

  private SourcePolicy createSourcePolicy(Policy policy) {
    when(policyProvider.findSourceParameterizedPolicies(any())).thenReturn(singletonList(policy));
    return policyManager.createSourcePolicyInstance(flowOne, getEventMock(), publisher -> publisher,
                                                    mock(MessageSourceResponseParametersProcessor.class));
  }

  @Test
  public void operationPolicyAcceptsWhileAvailable() throws MuleException {
    startIfNeeded(policyManager);

    AtomicBoolean isOperationPolicyDisposed = new AtomicBoolean(false);

    trackPolicyDisposal(isOperationPolicyDisposed);

    final Policy policy = stubPolicy("PolicyStub");

    when(policyProvider.findOperationParameterizedPolicies(any())).thenReturn(asList(policy));

    clearPolicyManagerCaches();

    final InternalEvent event = mock(InternalEvent.class);

    OperationPolicyContext ctx = mock(OperationPolicyContext.class);
    when(event.getOperationPolicyContext()).thenReturn((EventInternalContext) ctx);

    OperationPolicy policyInstance =
        policyManager.createOperationPolicy(flowOne, event, mock(OperationParametersProcessor.class));

    clearPolicyManagerCaches();

    System.gc();

    // Verify that the previously obtained policyInstance is still good
    assertThat(isOperationPolicyDisposed.get(), is(false));

    policyInstance = null;

    verifyPolicyDisposal(isOperationPolicyDisposed);

    stopIfNeeded(policyManager);
  }

  /**
   * Overrides the policy factory in oder to track the disposal of a single policy obtained through the policy manager
   *
   * @param isPolicyDisposed Boolean that will be true once the obtained policy is disposed.
   * @see PolicyManager#createOperationPolicy(Component, CoreEvent, OperationParametersProcessor)
   * @see PolicyManager#createSourcePolicyInstance(Component, CoreEvent, ReactiveProcessor,
   *      MessageSourceResponseParametersProcessor)
   */
  private void trackPolicyDisposal(AtomicBoolean isPolicyDisposed) {
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

        return new DisposeListenerPolicy(operationPolicy, () -> {
          isPolicyDisposed.set(true);
          ((DeferredDisposable) operationPolicy).deferredDispose().dispose();
        });
      }

      @Override
      public SourcePolicy createSourcePolicy(List<Policy> innerKey, ReactiveProcessor flowExecutionProcessor,
                                             Optional<SourcePolicyParametersTransformer> lookupSourceParametersTransformer,
                                             SourcePolicyProcessorFactory sourcePolicyProcessorFactory,
                                             Function<MessagingException, MessagingException> resolver) {
        final SourcePolicy sourcePolicy =
            super.createSourcePolicy(innerKey, flowExecutionProcessor, lookupSourceParametersTransformer,
                                     sourcePolicyProcessorFactory, resolver);

        return new DisposeListenerPolicy(sourcePolicy, () -> {
          isPolicyDisposed.set(true);
          ((DeferredDisposable) sourcePolicy).deferredDispose().dispose();
        });
      }
    });
  }

  private void verifyPolicyDisposal(AtomicBoolean isPolicyDisposed) {
    // Verify that dispose is called when the policy is no longer being used
    new PollingProber(GC_POLLING_TIMEOUT, DEFAULT_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      System.gc();
      return isPolicyDisposed.get();
    }));
  }

  private void verifyActivePolicies(int policyCount) {
    // Verify that dispose is called when the policy is no longer being used
    new PollingProber(GC_POLLING_TIMEOUT, DEFAULT_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      System.gc();
      return policyManager.getActivePoliciesCount() == policyCount;
    }));
  }

  @Test
  public void cachesEvictedWhileLookingForPolicies() throws InterruptedException {
    final Policy policy = mock(Policy.class, RETURNS_DEEP_STUBS);
    when(policyProvider.isSourcePoliciesAvailable()).thenReturn(true);
    when(policyProvider.findSourceParameterizedPolicies(any())).thenReturn(asList(policy));
    clearPolicyManagerCaches();

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
        clearPolicyManagerCaches();
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
    clearPolicyManagerCaches();

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

  /**
   * Stubs a Policy, instead of mocking it. Mocking a policy would imply invocation interceptions of the form:
   *
   * <pre>
   * {@code
   * when(mockedCall).thenAnswer(invocation -> {
   *    // Invocation arguments access
   * }));}
   * </pre>
   *
   * This can be problematic given that references to that arguments are maintained at the mocks and the
   * {@link DefaultPolicyManager} functioning can be unexpectedly affected.
   *
   * @see DefaultPolicyManager
   * @return A {@link Policy} stub.
   */
  private Policy stubPolicy(String policyId) {
    PolicyChain policyChain = new PolicyChain() {

      @Override
      public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
        return Flux.from(publisher);
      }

      @Override
      public ProcessingStrategy getProcessingStrategy() {
        return DirectProcessingStrategyFactory.DIRECT_PROCESSING_STRATEGY_INSTANCE;
      }
    };
    return new Policy(policyChain, policyId);
  }

  private static class DisposeListenerPolicy implements SourcePolicy, OperationPolicy, Disposable, DeferredDisposable {

    private final Either<SourcePolicy, OperationPolicy> policy;
    private final Disposable deferredDispose;

    public DisposeListenerPolicy(SourcePolicy sourcePolicy, Disposable deferredDispose) {
      policy = left(sourcePolicy);
      this.deferredDispose = deferredDispose;
    }

    public DisposeListenerPolicy(OperationPolicy operationPolicy, Disposable deferredDispose) {
      policy = right(operationPolicy);
      this.deferredDispose = deferredDispose;
    }

    @Override
    public void process(CoreEvent sourceEvent, MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor,
                        CompletableCallback<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> callback) {
      policy.applyLeft(sourcePolicy -> sourcePolicy.process(sourceEvent, messageSourceResponseParametersProcessor, callback));
    }

    @Override
    public void process(CoreEvent operationEvent, OperationExecutionFunction operationExecutionFunction,
                        OperationParametersProcessor parametersProcessor, ComponentLocation componentLocation,
                        ExecutorCallback callback) {
      policy.applyRight(operationPolicy -> operationPolicy.process(operationEvent, operationExecutionFunction,
                                                                   parametersProcessor, componentLocation, callback));
    }

    @Override
    public Disposable deferredDispose() {
      return deferredDispose;
    }

    @Override
    public void dispose() {
      deferredDispose.dispose();
    }
  }
}

