/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.runtime.api.notification.FlowConstructNotification.FLOW_CONSTRUCT_STOPPED;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.internal.policy.SourcePolicyContext.SOURCE_POLICY_CONTEXT;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.FlowConstructNotification;
import org.mule.runtime.api.notification.FlowConstructNotificationListener;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.MuleContext;
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
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
public class DefaultPolicyManager implements PolicyManager, Initialisable, Disposable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPolicyManager.class);

  private static final OperationPolicy NO_POLICY_OPERATION =
      (operationEvent, operationExecutionFunction, opParamProcessor, componentLocation, callback) -> operationExecutionFunction
          .execute(opParamProcessor.getOperationParameters(), operationEvent, callback);

  @Inject
  private ErrorTypeLocator errorTypeLocator;

  @Inject
  private Collection<ExceptionContextProvider> exceptionContextProviders;

  @Inject
  private ServerNotificationManager notificationManager;

  private MuleContext muleContext;

  private Registry registry;

  private final AtomicBoolean isPoliciesAvailable = new AtomicBoolean(false);

  private final Executor syncExecutor = Runnable::run;

  private final Cache<String, SourcePolicy> noPolicySourceInstances =
      Caffeine.newBuilder()
          .executor(syncExecutor)
          .removalListener((key, value, cause) -> disposeIfNeeded(value, LOGGER))
          .build();

  // These next caches contain the Composite Policies for a given sequence of policies to be applied.

  private final Cache<Pair<String, List<Policy>>, SourcePolicy> sourcePolicyInnerCache =
      Caffeine.newBuilder()
          .executor(syncExecutor)
          .removalListener((key, value, cause) -> disposeIfNeeded(value, LOGGER))
          .build();
  private final Cache<List<Policy>, OperationPolicy> operationPolicyInnerCache =
      Caffeine.newBuilder()
          .executor(syncExecutor)
          .removalListener((key, value, cause) -> disposeIfNeeded(value, LOGGER))
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

    final SourcePolicyContext ctx = SourcePolicyContext.from(sourceEvent);
    final PolicyPointcutParameters sourcePointcutParameters = ctx.getPointcutParameters();

    final Pair<String, PolicyPointcutParameters> policyKey =
        new Pair<>(source.getLocation().getRootContainerName(), sourcePointcutParameters);

    final SourcePolicy policy = sourcePolicyOuterCache.getIfPresent(policyKey);
    if (policy != null) {
      return policy;
    }

    return sourcePolicyOuterCache.get(policyKey, outerKey -> sourcePolicyInnerCache
        .get(new Pair<>(source.getLocation().getRootContainerName(),
                        policyProvider.findSourceParameterizedPolicies(sourcePointcutParameters)),
             innerKey -> innerKey.getSecond().isEmpty()
                 ? new NoSourcePolicy(flowExecutionProcessor)
                 : new CompositeSourcePolicy(innerKey.getSecond(), flowExecutionProcessor,
                                             lookupSourceParametersTransformer(sourceIdentifier),
                                             sourcePolicyProcessorFactory, exception -> new MessagingExceptionResolver(source)
                                                 .resolve(exception, errorTypeLocator, exceptionContextProviders))));
  }

  @Override
  public PolicyPointcutParameters addSourcePointcutParametersIntoEvent(Component source, TypedValue<?> attributes,
                                                                       InternalEvent.Builder eventBuilder) {
    final PolicyPointcutParameters sourcePolicyParams =
        policyPointcutParametersManager.createSourcePointcutParameters(source, attributes);
    eventBuilder.addInternalParameter(SOURCE_POLICY_CONTEXT, new SourcePolicyContext(sourcePolicyParams));
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

    return operationPolicyOuterCache.get(policyKey, outerKey -> operationPolicyInnerCache
        .get(policyProvider.findOperationParameterizedPolicies(outerKey.getSecond()),
             innerKey -> innerKey.isEmpty()
                 ? NO_POLICY_OPERATION
                 : new CompositeOperationPolicy(operation, innerKey,
                                                lookupOperationParametersTransformer(outerKey.getFirst()),
                                                operationPolicyProcessorFactory,
                                                muleContext.getConfiguration().getShutdownTimeout(),
                                                muleContext.getSchedulerService()
                                                    .ioScheduler(muleContext.getSchedulerBaseConfig().withMaxConcurrentTasks(1)
                                                        .withName(operation.getLocation().getLocation() + ".policy.flux.")))));
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

    // Invalidate from outer "with policy cache"
    sourcePolicyOuterCache.asMap().keySet().stream()
        .filter(pair -> pair.getFirst().equals(flowName))
        .forEach(matchingPair -> sourcePolicyOuterCache.invalidate(matchingPair));

    // Invalidate from inner "with policy cache"
    sourcePolicyInnerCache.asMap().keySet().stream()
        .filter(pair -> pair.getFirst().equals(flowName))
        .forEach(matchingPair -> sourcePolicyInnerCache.invalidate(matchingPair));

  }

  @Override
  public void dispose() {
    evictCaches();
  }

  private void evictCaches() {
    noPolicySourceInstances.invalidateAll();

    sourcePolicyOuterCache.invalidateAll();
    operationPolicyOuterCache.invalidateAll();

    sourcePolicyInnerCache.invalidateAll();
    operationPolicyInnerCache.invalidateAll();
  }

  @Inject
  public void setRegistry(Registry registry) {
    this.registry = registry;
  }

  @Inject
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
}
