/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.producer;

import static org.mule.runtime.core.internal.config.togglz.MuleTogglzFeatureManagerProvider.FEATURE_PROVIDER;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingProducerScope;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ExtensionProfilingEventContext;
import org.mule.runtime.core.internal.config.FeatureFlaggingUtils;
import org.mule.runtime.core.internal.config.togglz.user.MuleTogglzArtifactFeatureUser;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.config.togglz.MuleTogglzProfilingFeature;
import org.mule.runtime.core.internal.profiling.ResettableProfilingDataProducer;

import java.util.Collection;
import java.util.function.Function;

/**
 * A {@link ProfilingDataProducer} for producing data related to extensions.
 *
 * @see org.mule.runtime.api.profiling.type.ExtensionProfilingEventType
 * @since 4.4
 */
public class ExtensionProfilingDataProducer
    implements ResettableProfilingDataProducer<ExtensionProfilingEventContext, Object> {

  private final DefaultProfilingService defaultProfilingService;
  private final ProfilingEventType<ExtensionProfilingEventContext> profilingEventType;
  private final ProfilingProducerScope profilingProducerContext;
  private ProfilingDataProducerStatus profilingProducerStatus;


  public ExtensionProfilingDataProducer(DefaultProfilingService defaultProfilingService,
                                        ProfilingEventType<ExtensionProfilingEventContext> profilingEventType,
                                        ProfilingProducerScope profilingProducerContext) {
    this.defaultProfilingService = defaultProfilingService;
    this.profilingEventType = profilingEventType;
    this.profilingProducerContext = profilingProducerContext;
  }

  @Override
  public void triggerProfilingEvent(ExtensionProfilingEventContext profilingEventContext) {
    if (profilingProducerStatus == null) {
      reset();
    }

    if (profilingProducerStatus.isEnabled()) {
      defaultProfilingService.notifyEvent(profilingEventContext, profilingEventType);
    }
  }

  @Override
  public void triggerProfilingEvent(Object sourceData, Function<Object, ExtensionProfilingEventContext> transformation) {
    if (profilingProducerStatus == null) {
      reset();
    }

    if (profilingProducerStatus.isEnabled()) {
      defaultProfilingService.notifyEvent(transformation.apply(sourceData), profilingEventType);
    }
  }

  @Override
  public void reset() {
    FeatureFlaggingUtils
        .withFeatureUser(new MuleTogglzArtifactFeatureUser(profilingProducerContext.getProducerScopeIdentifier()),
                         this::resetStatus);
  }

  private void resetStatus() {
    Collection<MuleTogglzProfilingFeature> profilingFeatures = FEATURE_PROVIDER.getConsumerFeaturesFor(profilingEventType);
    this.profilingProducerStatus = new ProfilingDataProducerStatus(profilingFeatures);
  }

}
