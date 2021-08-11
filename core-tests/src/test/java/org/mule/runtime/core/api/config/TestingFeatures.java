/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.config.Feature;

import java.util.Optional;

public enum TestingFeatures implements Feature {

  TESTING_FEATURE("Testing feature", "MULE-123", "4.4.0"),

  TESTING_FEATURE_OVERRIDDEN_WITH_SYSTEM_PROPERTY("Testing feature", "MULE-123", "4.4.0", "mule.testing.feature-flagging");

  private final String description;
  private final String issue;
  private final String enabledByDefaultSince;
  private final String overridingSystemPropertyName;

  TestingFeatures(String description, String issue, String enabledByDefaultSince) {
    this(description, issue, enabledByDefaultSince, null);
  }

  TestingFeatures(String description, String issue, String enabledByDefaultSince, String overridingSystemPropertyName) {
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
