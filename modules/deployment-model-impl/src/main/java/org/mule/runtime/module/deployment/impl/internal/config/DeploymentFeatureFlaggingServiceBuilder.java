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
 * This builder creates a {@link FeatureFlaggingService}.
 *
 * @see FeatureFlaggingService
 * 
 * @since 4.4.0
 */
public final class DeploymentFeatureFlaggingServiceBuilder {

  private static final Logger LOGGER = getLogger(DeploymentFeatureFlaggingServiceBuilder.class);

  private ArtifactDescriptor artifactDescriptor;
  private MuleContext muleContext;

  private final Map<Feature, Predicate<FeatureContext>> artifactConfigurations = new HashMap<>();
  private final Map<Feature, Predicate<MuleContext>> contextConfigurations = new HashMap<>();

  public DeploymentFeatureFlaggingServiceBuilder withDescriptor(ArtifactDescriptor artifactDescriptor) {
    this.artifactDescriptor = artifactDescriptor;
    return this;
  }

  public DeploymentFeatureFlaggingServiceBuilder withMuleContext(MuleContext context) {
    this.muleContext = context;
    return this;
  }

  public DeploymentFeatureFlaggingServiceBuilder artifactConfigurations(Map<Feature, Predicate<FeatureContext>> configurations) {
    this.artifactConfigurations.putAll(configurations);
    return this;
  }

  public DeploymentFeatureFlaggingServiceBuilder contextConfigurations(Map<Feature, Predicate<MuleContext>> contextConfigurations) {
    this.contextConfigurations.putAll(contextConfigurations);
    return this;
  }

  public FeatureFlaggingService build() {
    Map<Feature, Boolean> features = new HashMap<>();
    LOGGER.debug("Configuring feature flags...");
    if (artifactDescriptor != null) {
      FeatureContext featureContext = new FeatureContext(artifactDescriptor.getMinMuleVersion());
      artifactConfigurations.forEach((feature, artifactDescriptorPredicate) -> features
          .put(feature, isFeatureEnabled(feature, featureContext, artifactDescriptorPredicate)));
    }
    if (muleContext != null) {
      contextConfigurations.forEach((feature, artifactDescriptorPredicate) -> features
          .put(feature, isFeatureEnabled(feature, muleContext, artifactDescriptorPredicate)));
    }
    return new DefaultFeatureFlaggingService(features);
  }

  private <T> boolean isFeatureEnabled(Feature feature, T featureContext, Predicate<T> featurePredicate) {
    boolean enabled;
    Optional<String> systemPropertyName = feature.getOverridingSystemPropertyName();
    if (systemPropertyName.isPresent() && getProperty(systemPropertyName.get()) != null) {
      enabled = getBoolean(systemPropertyName.get());
      LOGGER.debug("Setting feature {} = {} for artifact [{}] because of System Property '{}'", feature, enabled,
                   artifactDescriptor.getName(),
                   systemPropertyName);
    } else {
      enabled = featurePredicate.test(featureContext);
      LOGGER.debug("Setting feature {} = {} for artifact [{}]", feature, enabled, artifactDescriptor.getName());
    }
    return enabled;
  }

}
