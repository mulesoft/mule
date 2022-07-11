/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.runtime.api.notification.FlowConstructNotification.FLOW_CONSTRUCT_STOPPED;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;

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
import org.mule.runtime.core.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyProvider;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.policy.api.OperationPolicyPointcutParametersFactory;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.runtime.policy.api.SourcePolicyPointcutParametersFactory;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;

import org.checkerframework.checker.nullness.qual.NonNull;
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
      (operationEvent, operationExecutionFunction, opParamProcessor, componentLocation, callback) -> operationExecutionFunction
          .execute(opParamProcessor.getOperationParameters(), operationEvent, callback);

  /**
   * @return A no-op policy that will directly execute the operation function.
   */
  public static OperationPolicy noPolicyOperation() {
    return NO_POLICY_OPERATION;
  }

  /**
   * @param policy the {@link OperationPolicy} to evaluate
   * @return {@code true} if the provided policy is a no-op, {@code false} if a policy is actually applied.
   */
  public static boolean isNoPolicyOperation(OperationPolicy policy) {
    return NO_POLICY_OPERATION.equals(policy);
  }

  @Inject
  private ErrorTypeLocator errorTypeLocator;

  @Inject
  private Collection<ExceptionContextProvider> exceptionContextProviders;

  @Inject
  private ServerNotificationManager notificationManager;

  private MuleContext muleContext;

  private Registry registry;

  private CompositePolicyFactory compositePolicyFactory = new CompositePolicyFactory();

  private final AtomicBoolean isSourcePoliciesAvailable = new AtomicBoolean(false);
  private final AtomicBoolean isOperationPoliciesAvailable = new AtomicBoolean(false);

  // This set holds the references that are needed to do the dispose after the referenced policy is no longer used.
  private final ReferenceQueue<DeferredDisposable> stalePoliciesQueue = new ReferenceQueue<>();

  private final Set<DeferredDisposableWeakReference> activePolicies = new HashSet<>();

  private final ReentrantReadWriteLock cacheInvalidateLock = new ReentrantReadWriteLock();

  private volatile boolean stopped = true;
  private Future<?> taskHandle;
  @Inject
  private SchedulerService schedulerService;
  private Scheduler scheduler;

  private final Cache<String, SourcePolicy> noPolicySourceInstances =
      Caffeine.newBuilder()
          .build();

  // These next caches contain the Composite Policies for a given sequence of policies to be applied.

  private final Cache<Pair<String, List<Policy>>, SourcePolicy> sourcePolicyInnerCache =
      Caffeine.newBuilder()
          .build();
  private final Cache<Pair<String, List<Policy>>, OperationPolicy> operationPolicyInnerCache =
      Caffeine.newBuilder()
          .build();

  // These next caches cache the actual composite policies for a given parameters. Since many parameters combinations may result
  // in a same set of policies to be applied, many entries of this cache may reference the same composite policy instance.

  private Cache<Pair<String, PolicyPointcutParameters>, SourcePolicy> sourcePolicyOuterCache =
      Caffeine.newBuilder()
          .expireAfterAccess(60, SECONDS)
          .build();
  private Cache<Pair<String, PolicyPointcutParameters>, OperationPolicy> operationPolicyOuterCache =
      Caffeine.newBuilder()
          .expireAfterAccess(60, SECONDS)
          .build();

  private PolicyProvider policyProvider;
  private OperationPolicyProcessorFactory operationPolicyProcessorFactory;
  private SourcePolicyProcessorFactory sourcePolicyProcessorFactory;

  private PolicyPointcutParametersManager policyPointcutParametersManager;

  @Inject
  private FeatureFlaggingService featureFlaggingService;

  @Override
  public SourcePolicy createSourcePolicyInstance(Component source, CoreEvent sourceEvent,
                                                 ReactiveProcessor flowExecutionProcessor,
                                                 MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
    final ComponentIdentifier sourceIdentifier = source.getLocation().getComponentIdentifier().getIdentifier();

    if (!isSourcePoliciesAvailable.get()) {
      final SourcePolicy policy = noPolicySourceInstances.getIfPresent(source.getRootContainerLocation().getGlobalName());

      if (policy != null) {
        return policy;
      }

      return noPolicySourceInstances.get(source.getLocation().getRootContainerName(),
                                         k -> new NoSourcePolicy(flowExecutionProcessor));
    }

    final SourcePolicyContext ctx = SourcePolicyContext.from(sourceEvent);
    final PolicyPointcutParameters sourcePointcutParameters = ctx.getPointcutParameters();

    final Pair<String, PolicyPointcutParameters> policyKey =
        new Pair<>(source.getLocation().getRootContainerName(), sourcePointcutParameters);

    final SourcePolicy policy = sourcePolicyOuterCache.getIfPresent(policyKey);
    if (policy != null) {
      return policy;
    }

    // Although cache is being written in the locked section, read Lock is being used since the intention is to avoid cache being
    // invalidated while being populated and not to avoid multiple threads populating it at the same time.
    cacheInvalidateLock.readLock().lock();

    try {
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
                                                                   .resolve(exception, errorTypeLocator,
                                                                            exceptionContextProviders))));

      activePolicies.add(new DeferredDisposableWeakReference((DeferredDisposable) sourcePolicy, stalePoliciesQueue));

      return sourcePolicy;
    } finally {
      cacheInvalidateLock.readLock().unlock();
    }
  }

  @Override
  public PolicyPointcutParameters addSourcePointcutParametersIntoEvent(Component source, TypedValue<?> attributes,
                                                                       InternalEvent event) {
    final PolicyPointcutParameters sourcePolicyParams =
        policyPointcutParametersManager.createSourcePointcutParameters(source, attributes);
    event.setSourcePolicyContext(new SourcePolicyContext(sourcePolicyParams));
    return sourcePolicyParams;
  }

  @Override
  public OperationPolicy createOperationPolicy(Component operation, CoreEvent event,
                                               OperationParametersProcessor operationParameters) {
    if (!isOperationPoliciesAvailable.get()) {
      return NO_POLICY_OPERATION;
    }

    PolicyPointcutParameters operationPointcutParameters =
        policyPointcutParametersManager.createOperationPointcutParameters(operation, event,
                                                                          operationParameters.getOperationParameters());

    final String operationLocation = operation.getLocation().getLocation();
    final ComponentIdentifier operationIdentifier = operation.getLocation().getComponentIdentifier().getIdentifier();
    final Pair<String, PolicyPointcutParameters> policyKey =
        new Pair<>(operationLocation, operationPointcutParameters);

    final OperationPolicy policy = operationPolicyOuterCache.getIfPresent(policyKey);
    if (policy != null) {
      return policy;
    }

    // Although cache is being written in the locked section, read Lock is being used since the intention is to avoid cache being
    // invalidated while being populated and not to avoid multiple threads populating it at the same time.
    cacheInvalidateLock.readLock().lock();

    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Operation policy - populating outer cache for {}", policyKey);
      }

      OperationPolicy operationPolicy =
          operationPolicyOuterCache.get(policyKey, outerKey -> operationPolicyInnerCache
              .get(new Pair<>(operationLocation, policyProvider.findOperationParameterizedPolicies(outerKey.getSecond())),
                   innerKey -> innerKey.getSecond().isEmpty()
                       ? NO_POLICY_OPERATION
                       : compositePolicyFactory.createOperationPolicy(operation, innerKey.getSecond(),
                                                                      lookupOperationParametersTransformer(operationIdentifier),
                                                                      operationPolicyProcessorFactory,
                                                                      muleContext.getConfiguration().getShutdownTimeout(),
                                                                      muleContext.getSchedulerService()
                                                                          .ioScheduler(muleContext.getSchedulerBaseConfig()
                                                                              .withMaxConcurrentTasks(1)
                                                                              .withName(operation.getLocation().getLocation()
                                                                                  + ".policy.flux.")),
                                                                      featureFlaggingService)));

      if (operationPolicy instanceof DeferredDisposable) {
        activePolicies.add(new DeferredDisposableWeakReference((DeferredDisposable) operationPolicy, stalePoliciesQueue));
      }

      return operationPolicy;
    } finally {
      cacheInvalidateLock.readLock().unlock();
    }
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
        isSourcePoliciesAvailable.set(policyProvider.isSourcePoliciesAvailable());
        isOperationPoliciesAvailable.set(policyProvider.isOperationPoliciesAvailable());
      });

      isSourcePoliciesAvailable.set(policyProvider.isSourcePoliciesAvailable());
      isOperationPoliciesAvailable.set(policyProvider.isOperationPoliciesAvailable());
    }

    policyPointcutParametersManager =
        new PolicyPointcutParametersManager(registry.lookupAllByType(SourcePolicyPointcutParametersFactory.class),
                                            registry.lookupAllByType(OperationPolicyPointcutParametersFactory.class));

    // Register flow disposal listener
    notificationManager.addListener(new FlowConstructNotificationListener<FlowConstructNotification>() {

      @Override
      public boolean isBlocking() {
        return false;
      }

      @Override
      public void onNotification(FlowConstructNotification notification) {
        if (Integer.parseInt(notification.getAction().getIdentifier()) == FLOW_CONSTRUCT_STOPPED) {
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

    try {
      while (stalePoliciesQueue.remove(1) != null) {
        // nothing to do, just the removal
      }
    } catch (InterruptedException e) {
      currentThread().interrupt();
      throw new MuleRuntimeException(e);
    } catch (IllegalArgumentException e) {
      if (getProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR) != null) {
        throw new MuleRuntimeException(e);
      } else {
        LOGGER.warn("Exception when disposing DefaultPolicyManager", e);
      }
    }

    evictCaches();
    scheduler.stop();

    activePolicies.clear();
  }

  private void disposePolicies() {
    noPolicySourceInstances.asMap().values().forEach(policy -> {
      clearActive(policy);
      disposeIfNeeded(policy, LOGGER);
    });
    sourcePolicyInnerCache.asMap().values().forEach(policy -> {
      clearActive(policy);
      disposeIfNeeded(policy, LOGGER);
    });
    operationPolicyInnerCache.asMap().values().forEach(policy -> {
      clearActive(policy);
      disposeIfNeeded(policy, LOGGER);
    });
  }

  private void clearActive(@NonNull Object policy) {
    for (Iterator<DeferredDisposableWeakReference> iterator = activePolicies.iterator(); iterator.hasNext();) {
      if (policy == iterator.next().get()) {
        iterator.remove();
      }
    }
  }

  private void evictCaches() {
    cacheInvalidateLock.writeLock().lock();

    try {
      noPolicySourceInstances.invalidateAll();

      sourcePolicyInnerCache.invalidateAll();
      operationPolicyInnerCache.invalidateAll();

      sourcePolicyOuterCache.invalidateAll();
      operationPolicyOuterCache.invalidateAll();
    } finally {
      cacheInvalidateLock.writeLock().unlock();
    }
  }

  @Inject
  public void setRegistry(Registry registry) {
    this.registry = registry;
  }

  @Inject
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
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

  // Testing purposes
  void setOuterCachesExpireTime(int timeout, TimeUnit timeUnit) {
    sourcePolicyOuterCache = Caffeine.newBuilder()
        .expireAfterAccess(timeout, timeUnit)
        .build();
    operationPolicyOuterCache = Caffeine.newBuilder()
        .expireAfterAccess(timeout, timeUnit)
        .build();
  }

  int getActivePoliciesCount() {
    return activePolicies.size();
  }

  private static final class DeferredDisposableWeakReference extends WeakReference<DeferredDisposable> implements Disposable {

    private final Disposable deferredDispose;
    private final int hash;

    public DeferredDisposableWeakReference(DeferredDisposable referent, ReferenceQueue<? super DeferredDisposable> q) {
      super(referent, q);
      this.deferredDispose = referent.deferredDispose();
      this.hash = referent.hashCode();
    }

    @Override
    public void dispose() {
      deferredDispose.dispose();
    }

    /* MULE-18929: since outer cache has an expiring time but inner cache doesn't, we are could be creating
    * a new weak reference for the same policy. This will make that the activePolicies set will increase
    * its size for expired policies, unnecessary. Hence, overriding hashCode and equals methods to avoid having
    * more than one weak reference in the set */
    @Override
    public int hashCode() {
      return hash;
    }

    /* Important consideration: if the referent object is collected, it will be equal to NULL. Possible problem:
    *  if two collected referents had the same hash code (or simply generates a collision in the set) but where
    *  different objects, since we lost the objects (== null) for us will be both equal. This could be a conceptual
    *  problem since we would have "equivalent" different objects in a set, but considering our usage this won't be
    *  a problem: we use it to maintain the weak reference to dispose them (deferredDispose). So, when we add
    *  a weak reference to the set, its referent is obviously not null. When we remove them, if such collision happens,
    *  we will simply remove one of them (since equivalence), and in the next iteration we will remove the other one,
    *  independently of the hash implementation */
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof DeferredDisposableWeakReference)) {
        return false;
      }
      DeferredDisposable referent = this.get();
      DeferredDisposable otherReferent = ((DeferredDisposableWeakReference) o).get();
      if (referent != null) {
        return referent.equals(otherReferent);
      } else {
        return otherReferent == null;
      }
    }

  }
}
