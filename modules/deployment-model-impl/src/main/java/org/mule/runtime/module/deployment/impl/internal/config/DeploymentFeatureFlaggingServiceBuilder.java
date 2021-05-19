/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.config;

import org.mule.runtime.api.config.Feature;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.config.DefaultFeatureFlaggingService;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static java.lang.Boolean.getBoolean;
import static java.lang.System.getProperty;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>
 * This builder creates a {@link FeatureFlaggingService} whose {@link Feature} flags can be configured using a decoupled
 * {@link FeatureContext} (instead of the legacy {@link MuleContext} evaluation).
 * </p>
 * <p>
 * Maintains backward compatibility with {@link org.mule.runtime.core.internal.config.FeatureFlaggingServiceBuilder}.
 * </p>
 * 
 * @see FeatureFlaggingService
 * @see DeploymentFeatureFlaggingRegistry
 * @since 4.4.0
 */
public final class DeploymentFeatureFlaggingServiceBuilder {

  private static final Logger LOGGER = getLogger(DeploymentFeatureFlaggingServiceBuilder.class);

  private ArtifactDescriptor artifactDescriptor;
  private MuleContext muleContext;
  private String artifact = "";

  private final Map<Feature, Predicate<FeatureContext>> artifactConfigurations = new HashMap<>();
  private final Map<Feature, Predicate<MuleContext>> contextConfigurations = new HashMap<>();

  /**
   * Sets the {@link ArtifactDescriptor} that will be used for the {@link FeatureContext}, necessary for the decoupled
   * {@link #withFeatureContextConfigurations(Map)} evaluation.
   * 
   * @param artifactDescriptor {@link ArtifactDescriptor} that will be used for the {@link FeatureContext} construction.
   * @return This {@link DeploymentFeatureFlaggingServiceBuilder}.
   */
  public DeploymentFeatureFlaggingServiceBuilder withDescriptor(ArtifactDescriptor artifactDescriptor) {
    this.artifact = artifactDescriptor.getName();
    this.artifactDescriptor = artifactDescriptor;
    return this;
  }

  /**
   * Sets the {@link MuleContext} that will be used for the legacy {@link #withMuleContextConfigurations(Map)} evaluation.
   * 
   * @param muleContext {@link MuleContext} that will be used for the legacy {@link #withMuleContextConfigurations(Map)}
   *                    evaluation.
   * @return This {@link DeploymentFeatureFlaggingServiceBuilder}.
   */
  public DeploymentFeatureFlaggingServiceBuilder withMuleContext(MuleContext muleContext) {
    if (this.artifact.isEmpty()) {
      this.artifact = muleContext.getId();
    }
    this.muleContext = muleContext;
    return this;
  }

  /**
   * Feature flags that will be configured by evaluating a {@link FeatureContext}.
   * 
   * @param configurations Feature flags.
   * @see DeploymentFeatureFlaggingRegistry
   * @return This {@link DeploymentFeatureFlaggingServiceBuilder}.
   */
  public DeploymentFeatureFlaggingServiceBuilder withFeatureContextConfigurations(Map<Feature, Predicate<FeatureContext>> configurations) {
    this.artifactConfigurations.putAll(configurations);
    return this;
  }

  /**
   * Legacy feature flags that will be configured by evaluating a {@link MuleContext}.
   * 
   * @param configurations Legacy feature flags.
   * @return This {@link DeploymentFeatureFlaggingServiceBuilder}.
   */
  public DeploymentFeatureFlaggingServiceBuilder withMuleContextConfigurations(Map<Feature, Predicate<MuleContext>> configurations) {
    this.contextConfigurations.putAll(configurations);
    return this;
  }

  /**
   * Creates the {@link FeatureFlaggingService} instance configured by this {@link DeploymentFeatureFlaggingServiceBuilder}.
   * 
   * @return The {@link FeatureFlaggingService} instance.
   */
  public FeatureFlaggingService build() {
    Map<Feature, Boolean> features = new HashMap<>();
    LOGGER.debug("Configuring feature flags...");
    if (artifactDescriptor != null) {
      FeatureContext featureContext = new FeatureContext(artifactDescriptor.getMinMuleVersion());
      artifactConfigurations.forEach((feature, artifactDescriptorPredicate) -> features
          .put(feature, isFeatureFlagEnabled(feature, featureContext, artifactDescriptorPredicate)));
    }
    if (muleContext != null) {
      contextConfigurations.forEach((feature, artifactDescriptorPredicate) -> features
          .put(feature, isFeatureFlagEnabled(feature, muleContext, artifactDescriptorPredicate)));
    }
    return new DefaultFeatureFlaggingService(features);
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
                   artifact,
                   systemPropertyName);
    } else {
      enabled = featurePredicate.test(featureContext);
      LOGGER.debug("Setting feature {} = {} for artifact [{}]", feature, enabled, artifact);
    }
    return enabled;
  }

}
