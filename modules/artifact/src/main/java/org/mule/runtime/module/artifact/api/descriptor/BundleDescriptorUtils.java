/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.descriptor;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;

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

    Version available = getBundleVersion(availableVersion);
    Version expected = getBundleVersion(expectedVersion);

    if (available.compareTo(expected) >= 0) {
      String availableMajorVersion = getMajorVersion(availableVersion);
      String expectedMajorVersion = getMajorVersion(expectedVersion);

      return availableMajorVersion.equals(expectedMajorVersion);
    }

    return false;
  }

  private static String getMajorVersion(String version) {
    int index = version.indexOf(".");
    if (index < 0) {
      return version;
    } else {
      return version.substring(0, index);
    }
  }

  private static Version getBundleVersion(String version) {
    try {
      return new GenericVersionScheme().parseVersion(version);
    } catch (InvalidVersionSpecificationException e) {
      throw new InvalidDependencyVersionException("Unable to parse bundle version: " + version);
    }
  }
}
