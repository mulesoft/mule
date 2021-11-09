/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.producer;

import static org.mule.runtime.core.internal.config.FeatureFlaggingUtils.withFeatureUser;
import static org.mule.runtime.core.internal.config.togglz.MuleTogglzFeatureManagerProvider.FEATURE_PROVIDER;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingProducerScope;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.config.togglz.user.MuleTogglzArtifactFeatureUser;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.config.togglz.MuleTogglzProfilingFeature;
import org.mule.runtime.core.internal.profiling.ResettableProfilingDataProducer;
import org.mule.runtime.core.internal.profiling.context.DefaultComponentProcessingStrategyProfilingEventContext;

import java.util.Collection;
import java.util.function.Function;

/**
 * Default {@link ProfilingDataProducer} returned by a diagnostic service.
 *
 * @since 4.4
 */
public class ComponentProcessingStrategyProfilingDataProducer
    implements
    ResettableProfilingDataProducer<DefaultComponentProcessingStrategyProfilingEventContext, CoreEvent> {

  private final DefaultProfilingService defaultProfilingService;
  private final ProfilingEventType<ComponentProcessingStrategyProfilingEventContext> profilingEventType;
  private final ProfilingProducerScope profilingProducerContext;
  private ProfilingDataProducerStatus profilingProducerStatus;

  public ComponentProcessingStrategyProfilingDataProducer(DefaultProfilingService defaultProfilingService,
                                                          ProfilingEventType<ComponentProcessingStrategyProfilingEventContext> profilingEventType,
                                                          ProfilingProducerScope profilingProducerContext) {
    this.defaultProfilingService = defaultProfilingService;
    this.profilingEventType = profilingEventType;
    this.profilingProducerContext = profilingProducerContext;
    reset();
  }

  @Override
  public void triggerProfilingEvent(DefaultComponentProcessingStrategyProfilingEventContext profilingEventContext) {
    if (profilingProducerStatus.isEnabled()) {
      defaultProfilingService.notifyEvent(profilingEventContext, profilingEventType);
    }
  }

  @Override
  public void triggerProfilingEvent(CoreEvent sourceData,
                                    Function<CoreEvent, DefaultComponentProcessingStrategyProfilingEventContext> transformation) {
    if (profilingProducerStatus.isEnabled()) {
      defaultProfilingService.notifyEvent(transformation.apply(sourceData), profilingEventType);
    }
  }


  @Override
  public void reset() {
    withFeatureUser(new MuleTogglzArtifactFeatureUser(profilingProducerContext.getProducerScopeIdentifier()),
                    this::resetStatus);
  }

  private void resetStatus() {
    Collection<MuleTogglzProfilingFeature> profilingFeatures = FEATURE_PROVIDER.getConsumerFeaturesFor(profilingEventType);
    this.profilingProducerStatus = new ProfilingDataProducerStatus(profilingFeatures);
  }
}
