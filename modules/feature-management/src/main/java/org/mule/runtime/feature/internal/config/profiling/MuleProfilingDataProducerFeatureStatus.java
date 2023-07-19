/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.feature.internal.config.profiling;

import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.getFeatureState;
import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.withFeatureUser;

import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.feature.internal.togglz.MuleTogglzProfilingFeature;
import org.mule.runtime.feature.internal.togglz.provider.DefaultMuleTogglzFeatureProvider;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.user.FeatureUser;

import java.util.Collection;
import java.util.Set;

/**
 * The status of a {@link org.mule.runtime.api.profiling.ProfilingDataProducer} according to the requirements from the
 * {@link org.mule.runtime.api.profiling.ProfilingDataConsumer}
 */
public class MuleProfilingDataProducerFeatureStatus implements ProfilingDataProducerStatus {

  private final Set<FeatureState> profilingFeaturesStates = newKeySet();
  private final DefaultMuleTogglzFeatureProvider featureProvider;
  private final ProfilingEventType<?> profilingEventType;
  private final FeatureUser featureUser;

  public MuleProfilingDataProducerFeatureStatus(ProfilingEventType<?> profilingEventType,
                                                DefaultMuleTogglzFeatureProvider featureProvider,
                                                FeatureUser featureUser) {
    this.featureProvider = featureProvider;
    this.profilingEventType = profilingEventType;
    this.featureUser = featureUser;
    resetFeatureStates();
  }

  private void resetFeatureStates() {

    withFeatureUser(featureUser, () -> {
      Collection<MuleTogglzProfilingFeature> profilingFeatures = featureProvider.getConsumerFeaturesFor(profilingEventType);
      for (Feature feature : profilingFeatures) {
        profilingFeaturesStates.add(getFeatureState(feature));
      }
    });
  }

  @Override
  public boolean isEnabled() {
    return profilingFeaturesStates.stream().anyMatch(FeatureState::isEnabled);
  }

  @Override
  public void reset() {
    resetFeatureStates();
  }
}
