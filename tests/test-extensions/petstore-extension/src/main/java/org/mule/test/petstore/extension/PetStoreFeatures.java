/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.config.Feature;

import java.util.Optional;

import static java.util.Optional.ofNullable;

public enum PetStoreFeatures implements Feature {

  LEGACY_FEATURE_ONE("Feature legacy, just for testing", "MULE-123", "4.4.0", "mule.pet.store.legacy-behavior"),

  LEGACY_FEATURE_TWO("Feature legacy, just for testing", "MULE-123", "4.4.0", "mule.pet.store.legacy-behavior");

  private final String issue;
  private final String since;
  private final String description;
  private final String overridingSystemPropertyName;

  PetStoreFeatures(String description, String issue, String since) {
    this(description, issue, since, null);
  }

  PetStoreFeatures(String description, String issue, String since, String overridingSystemPropertyName) {
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
    return getEnabledByDefaultSince();
  }

  @Override
  public String getEnabledByDefaultSince() {
    return since;
  }

  @Override
  public Optional<String> getOverridingSystemPropertyName() {
    return ofNullable(overridingSystemPropertyName);
  }
}
