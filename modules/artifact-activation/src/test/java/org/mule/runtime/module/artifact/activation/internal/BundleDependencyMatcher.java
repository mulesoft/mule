/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.activation.internal;

import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class BundleDependencyMatcher extends TypeSafeMatcher<BundleDependency> {

  private final String expectedArtifactId;
  private final String expectedVersion;

  public static BundleDependencyMatcher bundleDependency(String expectedArtifactId) {
    return new BundleDependencyMatcher(expectedArtifactId, null);
  }

  public static BundleDependencyMatcher bundleDependency(String expectedArtifactId, String expectedVersion) {
    return new BundleDependencyMatcher(expectedArtifactId, expectedVersion);
  }

  private BundleDependencyMatcher(String expectedArtifactId, String expectedVersion) {
    this.expectedArtifactId = expectedArtifactId;
    this.expectedVersion = expectedVersion;
  }

  @Override
  protected boolean matchesSafely(BundleDependency bundleDependency) {
    boolean result = this.expectedArtifactId.equals(bundleDependency.getDescriptor().getArtifactId());
    if (this.expectedVersion != null) {
      result = result && this.expectedVersion.equals(bundleDependency.getDescriptor().getVersion());
    }
    return result;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText(expectedArtifactId + (expectedVersion != null ? "-" + expectedVersion : ""));
  }

  @Override
  protected void describeMismatchSafely(BundleDependency item, Description mismatchDescription) {
    mismatchDescription.appendText("got: " + item.getDescriptor());
  }

}
