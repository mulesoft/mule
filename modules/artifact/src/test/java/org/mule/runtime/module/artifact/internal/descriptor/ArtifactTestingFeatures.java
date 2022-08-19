/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.descriptor;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.config.Feature;
import org.mule.runtime.core.api.config.FeatureFlaggingRegistry;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Features meant to be used in {@link Feature} flag related tests.
 * 
 * @see FeatureFlaggingUtils
 * @see org.mule.runtime.api.config.FeatureFlaggingService
 * @since 4.5.0
 */
public enum ArtifactTestingFeatures implements Feature {

  ALWAYS_ON_FEATURE("Testing feature", "MULE-123", "4.4.0"), OVERRIDEABLE_FEATURE("Testing feature", "MULE-123", "4.4.0",
      "overrideable.feature.override");

  private static final AtomicBoolean areFeatureFlagsConfigured = new AtomicBoolean();
  static {
    if (!areFeatureFlagsConfigured.getAndSet(true)) {
      FeatureFlaggingRegistry.getInstance().registerFeatureFlag(ALWAYS_ON_FEATURE, featureContext -> true);
      FeatureFlaggingRegistry.getInstance().registerFeatureFlag(OVERRIDEABLE_FEATURE, featureContext -> false);
    }
  }

  public static final String OVERRIDEABLE_FEATURE_OVERRIDE = "overrideable.feature.override";
  private final String description;
  private final String issue;
  private final String enabledByDefaultSince;
  private final String overridingSystemPropertyName;

  ArtifactTestingFeatures(String description, String issue, String enabledByDefaultSince) {
    this(description, issue, enabledByDefaultSince, null);
  }

  ArtifactTestingFeatures(String description, String issue, String enabledByDefaultSince, String overridingSystemPropertyName) {
    this.description = description;
    this.issue = issue;
    this.enabledByDefaultSince = enabledByDefaultSince;
    this.overridingSystemPropertyName = overridingSystemPropertyName;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getIssueId() {
    return issue;
  }

  @Override
  public String getSince() {
    return getEnabledByDefaultSince();
  }

  @Override
  public String getEnabledByDefaultSince() {
    return enabledByDefaultSince;
  }

  @Override
  public Optional<String> getOverridingSystemPropertyName() {
    return ofNullable(overridingSystemPropertyName);
  }
}
