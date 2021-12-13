/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.producer.provider;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.EXTENSION_PROFILING_EVENT;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.FLOW_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.OPERATION_THREAD_RELEASE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getFullyQualifiedProfilingEventTypeIdentifier;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.ProfilingProducerScope;
import org.mule.runtime.api.profiling.threading.ThreadSnapshotCollector;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.core.internal.profiling.ComponentThreadingDataProducerProvider;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.profiling.ExtensionDataProducerProvider;
import org.mule.runtime.core.internal.profiling.ProcessingStratetegyDataProducerProvider;
import org.mule.runtime.core.internal.profiling.ProfilingDataProducerProvider;
import org.mule.runtime.core.internal.profiling.ResettableProfilingDataProducer;
import org.mule.runtime.feature.internal.config.profiling.ProfilingFeatureFlaggingService;

import java.util.HashMap;
import java.util.Map;

/**
 * Used for resolving data producers.
 *
 * @see DefaultProfilingService
 *
 * @since 4.5.0
 */
public class ProfilingDataProducerResolver {

  private final DefaultProfilingService profilingService;

  private final ThreadSnapshotCollector threadSnapshotCollector;

  private final Map<ProfilingEventType<? extends ProfilingEventContext>, ProfilingDataProducerProvider<?, ?>> profilingDataProducerProviders =
      new HashMap<>();

  private final ProfilingFeatureFlaggingService featureFlaggingService;

  public ProfilingDataProducerResolver(DefaultProfilingService profilingService,
                                       ThreadSnapshotCollector threadSnapshotCollector,
                                       ProfilingFeatureFlaggingService featureFlaggingService) {
    this.profilingService = profilingService;
    this.threadSnapshotCollector = threadSnapshotCollector;
    this.featureFlaggingService = featureFlaggingService;

    // We map each data profiling type to a data producer provider
    defineProfilerDataProducersProviders();
  }

  private void defineProfilerDataProducersProviders() {
    profilingDataProducerProviders.put(FLOW_EXECUTED, new ProcessingStratetegyDataProducerProvider(profilingService,
                                                                                                   FLOW_EXECUTED,
                                                                                                   featureFlaggingService));

    profilingDataProducerProviders
        .put(PS_SCHEDULING_FLOW_EXECUTION, new ProcessingStratetegyDataProducerProvider(profilingService,
                                                                                        PS_SCHEDULING_FLOW_EXECUTION,
                                                                                        featureFlaggingService));

    profilingDataProducerProviders
        .put(STARTING_FLOW_EXECUTION, new ProcessingStratetegyDataProducerProvider(profilingService,
                                                                                   STARTING_FLOW_EXECUTION,
                                                                                   featureFlaggingService));

    profilingDataProducerProviders
        .put(PS_FLOW_MESSAGE_PASSING, new ProcessingStratetegyDataProducerProvider(profilingService,
                                                                                   PS_FLOW_MESSAGE_PASSING,
                                                                                   featureFlaggingService));

    profilingDataProducerProviders
        .put(PS_OPERATION_EXECUTED, new ProcessingStratetegyDataProducerProvider(profilingService,
                                                                                 PS_OPERATION_EXECUTED,
                                                                                 featureFlaggingService));

    profilingDataProducerProviders
        .put(PS_SCHEDULING_OPERATION_EXECUTION, new ProcessingStratetegyDataProducerProvider(profilingService,
                                                                                             PS_SCHEDULING_OPERATION_EXECUTION,
                                                                                             featureFlaggingService));

    profilingDataProducerProviders
        .put(PS_STARTING_OPERATION_EXECUTION, new ProcessingStratetegyDataProducerProvider(profilingService,
                                                                                           PS_STARTING_OPERATION_EXECUTION,
                                                                                           featureFlaggingService));

    profilingDataProducerProviders
        .put(STARTING_OPERATION_EXECUTION, new ComponentThreadingDataProducerProvider(profilingService,
                                                                                      STARTING_OPERATION_EXECUTION,
                                                                                      threadSnapshotCollector,
                                                                                      featureFlaggingService));

    profilingDataProducerProviders
        .put(OPERATION_EXECUTED, new ComponentThreadingDataProducerProvider(profilingService,
                                                                            OPERATION_EXECUTED,
                                                                            threadSnapshotCollector,
                                                                            featureFlaggingService));

    profilingDataProducerProviders
        .put(OPERATION_THREAD_RELEASE, new ComponentThreadingDataProducerProvider(profilingService,
                                                                                  OPERATION_THREAD_RELEASE,
                                                                                  threadSnapshotCollector,
                                                                                  featureFlaggingService));

    profilingDataProducerProviders.put(EXTENSION_PROFILING_EVENT, new ExtensionDataProducerProvider(profilingService,
                                                                                                    EXTENSION_PROFILING_EVENT,
                                                                                                    featureFlaggingService));

  }

  public <T extends ProfilingEventContext, S> ResettableProfilingDataProducer<T, S> getProfilingDataProducer(
                                                                                                             ProfilingEventType<T> profilingEventType,
                                                                                                             ProfilingProducerScope producerScope) {
    return profilingDataProducerProviders.computeIfAbsent(profilingEventType,
                                                          profEventType -> {
                                                            throw new MuleRuntimeException(
                                                                                           (createStaticMessage(
                                                                                                                format("Profiling event type not registered: %s",
                                                                                                                       getFullyQualifiedProfilingEventTypeIdentifier(
                                                                                                                                                                     profilingEventType)))));
                                                          })
        .getProfilingDataProducer(producerScope);
  }
}
