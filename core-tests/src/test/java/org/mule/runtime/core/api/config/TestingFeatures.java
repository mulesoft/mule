/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import org.mule.runtime.api.config.Feature;

import java.util.Optional;

import static java.util.Optional.ofNullable;

public enum TestingFeatures implements Feature {

  TESTING_FEATURE("Testing feature", "MULE-123", "4.4.0"),

  TESTING_FEATURE_OVERRIDDEN_WITH_SYSTEM_PROPERTY("Testing feature", "MULE-123", "4.4.0", "mule.testing.feature-flagging");

  private final String description;
  private final String issue;
  private final String since;
  private final String overridingSystemPropertyName;

  TestingFeatures(String description, String issue, String since) {
    this(description, issue, since, null);
  }

  TestingFeatures(String description, String issue, String since, String overridingSystemPropertyName) {
    this.description = description;
    this.issue = issue;
    this.since = since;
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
    return since;
  }

  @Override
  public Optional<String> getOverridingSystemPropertyName() {
    return ofNullable(overridingSystemPropertyName);
  }
}
