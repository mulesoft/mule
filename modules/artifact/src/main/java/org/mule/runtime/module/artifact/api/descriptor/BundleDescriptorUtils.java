/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.descriptor;

import static com.vdurmont.semver4j.Semver.SemverType.LOOSE;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.SemverException;

/**
 * Utilities to work with {@link BundleDescriptor}
 */
public class BundleDescriptorUtils {

  private BundleDescriptorUtils() {}

  /**
   * Determines if a version is compatible with another one
   *
   * @param availableVersion version that is available to use. Non empty
   * @param expectedVersion version that is expected. Non empty
   * @return true if versions are compatible, false otherwise
   */
  public static boolean isCompatibleVersion(String availableVersion, String expectedVersion) {
    checkArgument(!isEmpty(availableVersion), "availableVersion cannot be empty");
    checkArgument(!isEmpty(expectedVersion), "expectedVersion cannot be empty");

    if (availableVersion.equals(expectedVersion)) {
      return true;
    }

    Semver available = getBundleVersion(availableVersion);
    Semver expected = getBundleVersion(expectedVersion);

    if (available.isGreaterThan(expected)) {
      return available.getMajor().equals(expected.getMajor());
    }

    return false;
  }

  /**
   * Determines if a bundle descriptor is compatible with another one.
   *
   * @param available bundle descriptor that is available to use.
   * @param expected bundle descriptor that is expected.
   * @return true if match in group and artifact id, have the same classifier and the versions are compatible, false otherwise.
   */
  public static boolean isCompatibleBundle(BundleDescriptor available, BundleDescriptor expected) {
    if (!available.getClassifier().equals(expected.getClassifier())) {
      return false;
    }

    if (!available.getGroupId().equals(expected.getGroupId())) {
      return false;
    }

    if (!available.getArtifactId().equals(expected.getArtifactId())) {
      return false;
    }

    return isCompatibleVersion(available.getVersion(), expected.getVersion());
  }

  private static Semver getBundleVersion(String version) {
    try {
      return new Semver(version, LOOSE);
    } catch (SemverException e) {
      throw new InvalidDependencyVersionException(
                                                  format("Unable to parse bundle version: %s, version is not following semantic versioning",
                                                         version),
                                                  e);
    }
  }
}
