/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal;

import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class BundleDependencyMatcher extends TypeSafeMatcher<BundleDependency> {

  private String expectedArtifactId;
  private String expectedVersion;

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
    description.appendText(expectedArtifactId);
  }

  @Override
  protected void describeMismatchSafely(BundleDependency item, Description mismatchDescription) {
    mismatchDescription.appendText("got: " + item.getDescriptor());
  }

}
