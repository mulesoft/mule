/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.runtime.api.notification.FlowConstructNotification.FLOW_CONSTRUCT_DISPOSED;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.internal.policy.PolicyPointcutParametersManager.POLICY_SOURCE_POINTCUT_PARAMETERS;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.FlowConstructNotification;
import org.mule.runtime.api.notification.FlowConstructNotificationListener;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyProvider;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.policy.api.OperationPolicyPointcutParametersFactory;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.runtime.policy.api.SourcePolicyPointcutParametersFactory;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Default implementation of {@link PolicyManager}.
 *
 * @since 4.0
 */
public class DefaultPolicyManager implements PolicyManager, Lifecycle {

  private static final long POLL_INTERVAL = SECONDS.toMillis(5);

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPolicyManager.class);

  private static final OperationPolicy NO_POLICY_OPERATION =
      (operationEvent, operationExecutionFunction, opParamProcessor, componentLocation) -> operationExecutionFunction
          .execute(opParamProcessor.getOperationParameters(), operationEvent);

  private MuleContext muleContext;

  private Registry registry;

  private CompositePolicyFactory compositePolicyFactory = new CompositePolicyFactory();

  private final AtomicBoolean isPoliciesAvailable = new AtomicBoolean(false);

  // This set holds the references that are needed to do the dispose after the referenced policy is no longer used.
  private final ReferenceQueue<DeferredDisposable> stalePoliciesQueue = new ReferenceQueue<>();

  private final Set<DeferredDisposableWeakReference> activePolicies = new HashSet<>();

  private volatile boolean stopped = true;
  private Future taskHandle;
  private SchedulerService schedulerService;
  private Scheduler scheduler;

  private final Cache<String, SourcePolicy> noPolicySourceInstances =
      Caffeine.newBuilder()
          .build();

  // These next caches contain the Composite Policies for a given sequence of policies to be applied.

  private final Cache<Pair<String, List<Policy>>, SourcePolicy> sourcePolicyInnerCache =
      Caffeine.newBuilder()
          .build();
  private final Cache<List<Policy>, OperationPolicy> operationPolicyInnerCache =
      Caffeine.newBuilder()
          .build();

  // These next caches cache the actual composite policies for a given parameters. Since many parameters combinations may result
  // in a same set of policies to be applied, many entries of this cache may reference the same composite policy instance.

  private final Cache<Pair<String, PolicyPointcutParameters>, SourcePolicy> sourcePolicyOuterCache =
      Caffeine.newBuilder()
          .expireAfterAccess(60, SECONDS)
          .build();
  private final Cache<Pair<ComponentIdentifier, PolicyPointcutParameters>, OperationPolicy> operationPolicyOuterCache =
      Caffeine.newBuilder()
          .expireAfterAccess(60, SECONDS)
          .build();

  private PolicyProvider policyProvider;
  private OperationPolicyProcessorFactory operationPolicyProcessorFactory;
  private SourcePolicyProcessorFactory sourcePolicyProcessorFactory;

  private PolicyPointcutParametersManager policyPointcutParametersManager;

  @Override
  public SourcePolicy createSourcePolicyInstance(Component source, CoreEvent sourceEvent,
                                                 ReactiveProcessor flowExecutionProcessor,
                                                 MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
    final ComponentIdentifier sourceIdentifier = source.getLocation().getComponentIdentifier().getIdentifier();

    if (!isPoliciesAvailable.get()) {
      final SourcePolicy policy = noPolicySourceInstances.getIfPresent(source.getLocation().getRootContainerName());

      if (policy != null) {
        return policy;
      }

      return noPolicySourceInstances.get(source.getLocation().getRootContainerName(),
                                         k -> new NoSourcePolicy(flowExecutionProcessor));
    }

    final PolicyPointcutParameters sourcePointcutParameters = ((InternalEvent) sourceEvent)
        .getInternalParameter(POLICY_SOURCE_POINTCUT_PARAMETERS);

    final Pair<String, PolicyPointcutParameters> policyKey =
        new Pair<>(source.getLocation().getRootContainerName(), sourcePointcutParameters);

    final SourcePolicy policy = sourcePolicyOuterCache.getIfPresent(policyKey);
    if (policy != null) {
      return policy;
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Source policy - populating outer cache for {}", policyKey);
    }

    SourcePolicy sourcePolicy = sourcePolicyOuterCache.get(policyKey, outerKey -> sourcePolicyInnerCache
        .get(new Pair<>(source.getLocation().getRootContainerName(),
                        policyProvider.findSourceParameterizedPolicies(sourcePointcutParameters)),
             innerKey -> innerKey.getSecond().isEmpty()
                 ? new NoSourcePolicy(flowExecutionProcessor)
                 : compositePolicyFactory.createSourcePolicy(innerKey.getSecond(), flowExecutionProcessor,
                                                             lookupSourceParametersTransformer(sourceIdentifier),
                                                             sourcePolicyProcessorFactory,
                                                             exception -> new MessagingExceptionResolver(source)
                                                                 .resolve(exception, muleContext))));

    activePolicies.add(new DeferredDisposableWeakReference((DeferredDisposable) sourcePolicy, stalePoliciesQueue));

    return sourcePolicy;
  }

  @Override
  public PolicyPointcutParameters addSourcePointcutParametersIntoEvent(Component source, TypedValue<?> attributes,
                                                                       InternalEvent.Builder eventBuilder) {
    final PolicyPointcutParameters sourcePolicyParams =
        policyPointcutParametersManager.createSourcePointcutParameters(source, attributes);
    eventBuilder.addInternalParameter(POLICY_SOURCE_POINTCUT_PARAMETERS, sourcePolicyParams);
    return sourcePolicyParams;
  }

  @Override
  public OperationPolicy createOperationPolicy(Component operation, CoreEvent event,
                                               OperationParametersProcessor operationParameters) {
    if (!isPoliciesAvailable.get()) {
      return NO_POLICY_OPERATION;
    }

    PolicyPointcutParameters operationPointcutParameters =
        policyPointcutParametersManager.createOperationPointcutParameters(operation, event,
                                                                          operationParameters.getOperationParameters());

    final ComponentIdentifier operationIdentifier = operation.getLocation().getComponentIdentifier().getIdentifier();
    final Pair<ComponentIdentifier, PolicyPointcutParameters> policyKey =
        new Pair<>(operationIdentifier, operationPointcutParameters);

    final OperationPolicy policy = operationPolicyOuterCache.getIfPresent(policyKey);
    if (policy != null) {
      return policy;
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Operation policy - populating outer cache for {}", policyKey);
    }

    final OperationPolicy operationPolicy = operationPolicyOuterCache.get(policyKey, outerKey -> operationPolicyInnerCache
        .get(policyProvider.findOperationParameterizedPolicies(outerKey.getSecond()),
             innerKey -> innerKey.isEmpty()
                 ? NO_POLICY_OPERATION
                 : compositePolicyFactory.createOperationPolicy(operation, innerKey,
                                                                lookupOperationParametersTransformer(outerKey.getFirst()),
                                                                operationPolicyProcessorFactory)));

    if (operationPolicy instanceof DeferredDisposable) {
      activePolicies.add(new DeferredDisposableWeakReference((DeferredDisposable) operationPolicy, stalePoliciesQueue));
    }

    return operationPolicy;
  }

  private Optional<OperationPolicyParametersTransformer> lookupOperationParametersTransformer(ComponentIdentifier componentIdentifier) {
    return registry.lookupAllByType(OperationPolicyParametersTransformer.class).stream()
        .filter(policyOperationParametersTransformer -> policyOperationParametersTransformer.supports(componentIdentifier))
        .findAny();
  }

  private Optional<SourcePolicyParametersTransformer> lookupSourceParametersTransformer(ComponentIdentifier componentIdentifier) {
    return registry.lookupAllByType(SourcePolicyParametersTransformer.class).stream()
        .filter(policyOperationParametersTransformer -> policyOperationParametersTransformer.supports(componentIdentifier))
        .findAny();
  }

  @Override
  public void initialise() throws InitialisationException {
    scheduler = schedulerService.customScheduler(SchedulerConfig.config()
        .withMaxConcurrentTasks(1)
        .withName("PolicyManager-StaleCleaner"));

    operationPolicyProcessorFactory = new DefaultOperationPolicyProcessorFactory();
    sourcePolicyProcessorFactory = new DefaultSourcePolicyProcessorFactory();

    policyProvider = registry.lookupByType(PolicyProvider.class).orElse(new NullPolicyProvider());

    if (muleContext.getArtifactType().equals(APP)) {
      policyProvider.onPoliciesChanged(() -> {
        evictCaches();
        isPoliciesAvailable.set(policyProvider.isPoliciesAvailable());
      });
    }

    isPoliciesAvailable.set(policyProvider.isPoliciesAvailable());

    policyPointcutParametersManager =
        new PolicyPointcutParametersManager(registry.lookupAllByType(SourcePolicyPointcutParametersFactory.class),
                                            registry.lookupAllByType(OperationPolicyPointcutParametersFactory.class));

    // Register flow disposal listener
    muleContext.getNotificationManager().addListener(new FlowConstructNotificationListener<FlowConstructNotification>() {

      @Override
      public boolean isBlocking() {
        return false;
      }

      @Override
      public void onNotification(FlowConstructNotification notification) {
        if (Integer.parseInt(notification.getAction().getIdentifier()) == FLOW_CONSTRUCT_DISPOSED) {
          LOGGER.debug("Invalidating flow from caches named {}", notification.getResourceIdentifier());
          // Invalidate flow from caches
          invalidateDisposedFlowFromCaches(notification.getResourceIdentifier());
        }
      }
    });
  }

  private void invalidateDisposedFlowFromCaches(String flowName) {
    // Invalidate from "no policy cache"
    noPolicySourceInstances.invalidate(flowName);

    // Invalidate from inner "with policy cache"
    sourcePolicyInnerCache.asMap().keySet().stream()
        .filter(pair -> pair.getFirst().equals(flowName))
        .forEach(matchingPair -> sourcePolicyInnerCache.invalidate(matchingPair));

    // Invalidate from outer "with policy cache"
    sourcePolicyOuterCache.asMap().keySet().stream()
        .filter(pair -> pair.getFirst().equals(flowName))
        .forEach(matchingPair -> sourcePolicyOuterCache.invalidate(matchingPair));
  }

  @Override
  public void start() throws MuleException {
    try {
      taskHandle = scheduler.submit(this::disposeStalePolicies);
    } catch (RejectedExecutionException e) {
      throw new MuleRuntimeException(e);
    }
    stopped = false;
  }

  @Override
  public void stop() throws MuleException {
    stopped = true;
    taskHandle.cancel(true);
    taskHandle = null;
  }

  @Override
  public void dispose() {
    disposePolicies();
    evictCaches();
    scheduler.stop();
  }

  private void disposePolicies() {
    noPolicySourceInstances.asMap().values().forEach(policy -> disposeIfNeeded(policy, LOGGER));
    sourcePolicyInnerCache.asMap().values().forEach(policy -> disposeIfNeeded(policy, LOGGER));
    operationPolicyInnerCache.asMap().values().forEach(policy -> disposeIfNeeded(policy, LOGGER));
  }

  private void evictCaches() {
    noPolicySourceInstances.invalidateAll();

    sourcePolicyInnerCache.invalidateAll();
    operationPolicyInnerCache.invalidateAll();

    sourcePolicyOuterCache.invalidateAll();
    operationPolicyOuterCache.invalidateAll();
  }

  @Inject
  public void setRegistry(Registry registry) {
    this.registry = registry;
  }

  @Inject
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Inject
  public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  public void setCompositePolicyFactory(CompositePolicyFactory compositePolicyFactory) {
    this.compositePolicyFactory = compositePolicyFactory;
  }

  private void disposeStalePolicies() {
    while (!stopped && !currentThread().isInterrupted()) {
      try {
        DeferredDisposableWeakReference stalePolicy = (DeferredDisposableWeakReference) stalePoliciesQueue.remove(POLL_INTERVAL);
        if (stalePolicy != null) {
          disposeIfNeeded(stalePolicy, LOGGER);
          activePolicies.remove(stalePolicy);
        }
      } catch (InterruptedException e) {
        currentThread().interrupt();
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Stale policies cleaner thread was interrupted. Finalizing.");
        }
      }
    }
  }

  private static final class DeferredDisposableWeakReference extends WeakReference<DeferredDisposable> implements Disposable {

    private final Disposable deferredDispose;

    public DeferredDisposableWeakReference(DeferredDisposable referent, ReferenceQueue<? super DeferredDisposable> q) {
      super(referent, q);
      this.deferredDispose = referent.deferredDispose();
    }

    @Override
    public void dispose() {
      deferredDispose.dispose();
    }
  }
}
