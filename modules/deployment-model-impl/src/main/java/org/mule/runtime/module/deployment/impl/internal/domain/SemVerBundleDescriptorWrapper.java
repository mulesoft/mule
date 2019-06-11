/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.domain;

import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import com.vdurmont.semver4j.Semver;

/**
 * Two instances of this class wrapping instances of {@link BundleDescriptor} that matches in all parameters but the minor and
 * the patch versions are equals and have the same hash.
 *
 * @since 4.2.2
 */
class SemVerBundleDescriptorWrapper {

  private final BundleDescriptor bundleDescriptor;

  SemVerBundleDescriptorWrapper(BundleDescriptor bundleDescriptor) {
    this.bundleDescriptor = bundleDescriptor;
  }

  SemVerBundleDescriptorWrapper(DomainDescriptor domainDescriptor) {
    this.bundleDescriptor = domainDescriptor.getBundleDescriptor();
  }

  @Override
  public int hashCode() {
    Semver version = new Semver(bundleDescriptor.getVersion());
    int result = bundleDescriptor.getGroupId().hashCode();
    result = 31 * result + bundleDescriptor.getArtifactId().hashCode();
    result = 31 * result + version.getMajor().hashCode();
    result = 31 * result + bundleDescriptor.getType().hashCode();
    return result;
  }

  @Override
  public boolean equals(Object otherObject) {
    if (this == otherObject) {
      return true;
    }

    if (!(otherObject instanceof SemVerBundleDescriptorWrapper)) {
      return false;
    }

    SemVerBundleDescriptorWrapper otherDescriptorWrapper = (SemVerBundleDescriptorWrapper) otherObject;

    BundleDescriptor myBundleDescriptor = this.bundleDescriptor;
    BundleDescriptor otherBundleDescriptor = otherDescriptorWrapper.bundleDescriptor;
    if (myBundleDescriptor == null && otherBundleDescriptor == null) {
      return true;
    }

    if (myBundleDescriptor == null || otherBundleDescriptor == null) {
      return false;
    }

    if (!myBundleDescriptor.getGroupId().equals(otherBundleDescriptor.getGroupId())) {
      return false;
    }

    if (!myBundleDescriptor.getArtifactId().equals(otherBundleDescriptor.getArtifactId())) {
      return false;
    }

    if (!myBundleDescriptor.getType().equals(otherBundleDescriptor.getType())) {
      return false;
    }

    Semver mySemver = new Semver(myBundleDescriptor.getVersion());
    Semver otherSemver = new Semver(otherBundleDescriptor.getVersion());
    return mySemver.getMajor().equals(otherSemver.getMajor());
  }

  @Override
  public String toString() {
    return this.bundleDescriptor.toString();
  }
}
