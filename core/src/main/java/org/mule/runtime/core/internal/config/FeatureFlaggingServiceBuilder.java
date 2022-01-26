/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.config;

import static java.lang.Boolean.getBoolean;
import static java.lang.System.getProperty;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getArtifactId;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.mule.runtime.api.config.Feature;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.config.FeatureContext;
import org.mule.runtime.feature.internal.config.DefaultFeatureFlaggingService;
import org.slf4j.Logger;

/**
 * This builder creates a {@link FeatureFlaggingService} whose {@link Feature} flags are set by evaluating the features registered
 * via {@link org.mule.runtime.core.api.config.FeatureFlaggingRegistry#registerFeatureFlag(Feature, Predicate)} against a
 * {@link FeatureContext}.
 * <p>
 * Maintains backward compatibility with a legacy {@link org.mule.runtime.api.meta.MuleVersion} evaluation.
 *
 * @see FeatureFlaggingService
 * @see org.mule.runtime.core.api.config.FeatureFlaggingRegistry
 *
 * @since 4.4.0
 */
public final class FeatureFlaggingServiceBuilder {

  private static final Logger LOGGER = getLogger(FeatureFlaggingServiceBuilder.class);

  private MuleContext muleContext;
  private FeatureContext featureContext;
  private String artifactName = "";

  private final Map<Feature, Predicate<FeatureContext>> featureContextFlags = new HashMap<>();
  private final Map<Feature, Predicate<MuleContext>> muleContextFlags = new HashMap<>();

  /**
   * Sets the {@link MuleContext} that will be used for the legacy {@link #withMuleContextFlags(Map)} evaluation.
   *
   * @param muleContext {@link MuleContext} that will be used for the legacy {@link #withMuleContextFlags(Map)} evaluation.
   * @return This {@link FeatureFlaggingServiceBuilder}.
   */
  public FeatureFlaggingServiceBuilder withContext(MuleContext muleContext) {
    if (this.artifactName.isEmpty()) {
      this.artifactName = muleContext.getId();
    }
    this.muleContext = muleContext;
    return this;
  }

  /**
   * Sets the {@link FeatureContext} that will be used for the {@link #withFeatureContextFlags(Map)} evaluation.
   *
   * @param featureContext {@link FeatureContext} that will be used for the legacy {@link #withFeatureContextFlags(Map)}
   *                       evaluation.
   * @return This {@link FeatureFlaggingServiceBuilder}.
   */
  public FeatureFlaggingServiceBuilder withContext(FeatureContext featureContext) {
    this.artifactName = featureContext.getArtifactName();
    this.featureContext = featureContext;
    return this;
  }

  /**
   * Features whose flags will be set by evaluating a predicate against a {@link FeatureContext}.
   *
   * @param configurations The features and their corresponding predicates.
   * @return This {@link FeatureFlaggingServiceBuilder}.
   */
  public FeatureFlaggingServiceBuilder withFeatureContextFlags(Map<Feature, Predicate<FeatureContext>> configurations) {
    this.featureContextFlags.putAll(configurations);
    return this;
  }

  /**
   * Features whose flags will be set by evaluating a predicate against a {@link MuleContext}.
   *
   * @param configurations The features and their corresponding predicates.
   * @return This {@link FeatureFlaggingServiceBuilder}.
   */
  public FeatureFlaggingServiceBuilder withMuleContextFlags(Map<Feature, Predicate<MuleContext>> configurations) {
    this.muleContextFlags.putAll(configurations);
    return this;
  }

  /**
   * Creates the {@link FeatureFlaggingService} instance configured by this {@link FeatureFlaggingServiceBuilder}.
   *
   * @return The {@link FeatureFlaggingService} instance.
   */
  public FeatureFlaggingService build() {
    Map<Feature, Boolean> features = new HashMap<>();
    LOGGER.debug("Configuring feature flags...");
    if (muleContext != null) {
      muleContextFlags.forEach((feature, artifactDescriptorPredicate) -> features
          .put(feature, isFeatureFlagEnabled(feature, muleContext, artifactDescriptorPredicate)));
    }
    if (featureContext != null) {
      featureContextFlags.forEach((feature, artifactDescriptorPredicate) -> features
          .put(feature, isFeatureFlagEnabled(feature, featureContext, artifactDescriptorPredicate)));
    }

    return new DefaultFeatureFlaggingService(artifactName, features);
  }

  /**
   * True if a feature flag is enabled under a determined feature context.
   *
   * @param feature          The feature whose feature flag must be evaluated.
   * @param featureContext   The feature context that must be used to set the feature flag.
   * @param featurePredicate The predicate that will be evaluated against the feature context.
   * @return True if a feature flag is enabled.
   */
  private <T> boolean isFeatureFlagEnabled(Feature feature, T featureContext, Predicate<T> featurePredicate) {
    boolean enabled;
    Optional<String> systemPropertyName = feature.getOverridingSystemPropertyName();
    if (systemPropertyName.isPresent() && getProperty(systemPropertyName.get()) != null) {
      enabled = getBoolean(systemPropertyName.get());
      LOGGER.debug("Setting feature {} = {} for artifact [{}] because of System Property '{}'", feature, enabled,
                   artifactName,
                   systemPropertyName);
    } else {
      enabled = featurePredicate.test(featureContext);
      LOGGER.debug("Setting feature {} = {} for artifact [{}]", feature, enabled, artifactName);
    }
    return enabled;
  }

}
