/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.version;

import static java.util.regex.Pattern.compile;

import static com.vdurmont.semver4j.Semver.SemverType.LOOSE;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vdurmont.semver4j.Semver;

public class VersionRange {

  public static final String VERSION_RANGE = "([\\[(])([^,\\[\\]()]*),([^,\\[\\]()]*)([\\])])";
  public static final String LOWER_BOUND_INCLUSIVE = "[";
  public static final String LOWER_BOUND_EXCLUSIVE = "(";
  public static final String UPPER_BOUND_INCLUSIVE = "]";
  public static final String UPPER_BOUND_EXCLUSIVE = ")";

  /**
   * pattern for extracting a ranges of versions. example: [1.5.0_11,1.6),[1.6.0_15,1.7),[1.7.0,] G1: [1.5.0_11,1.6) G2: [ G3:
   * 1.5.0_11 G4: 1.6 G5: ) G6: [1.6.0_15,1.7),[1.7.0,]
   */
  public static final Pattern VERSION_RANGES = compile("(" + VERSION_RANGE + "),?");
  public static final Pattern VALID_VERSION_RANGES = compile("^(?:" + VERSION_RANGE + ",?)+$");

  public static List<VersionRange> createVersionRanges(String versionsString) {
    if (!VALID_VERSION_RANGES.matcher(versionsString).matches()) {
      throw new IllegalArgumentException("Version range doesn't match pattern: " + VALID_VERSION_RANGES.pattern());
    }

    List<VersionRange> versions = new ArrayList<>();

    Matcher m = VERSION_RANGES.matcher(versionsString);
    while (m.find()) {
      versions.add(new VersionRange(m.group(1)));
    }

    return versions;
  }

  private boolean isLowerBoundInclusive = false;
  private boolean isUpperBoundInclusive = false;
  private String lowerVersion;
  private String upperVersion;

  public VersionRange(String versionRange) {
    Matcher m = VERSION_RANGES.matcher(versionRange);
    if (!m.matches()) {
      throw new IllegalArgumentException("Version range doesn't match pattern: " + VERSION_RANGES.pattern());
    }
    if (LOWER_BOUND_INCLUSIVE.equals(m.group(2))) {
      isLowerBoundInclusive = true;
    }
    if (UPPER_BOUND_INCLUSIVE.equals(m.group(5))) {
      isUpperBoundInclusive = true;
    }
    lowerVersion = m.group(3);
    upperVersion = m.group(4);
  }

  public String getLowerVersion() {
    return lowerVersion;
  }

  public String getUpperVersion() {
    return upperVersion;
  }

  public boolean isLowerBoundInclusive() {
    return isLowerBoundInclusive;
  }

  public boolean isUpperBoundInclusive() {
    return isUpperBoundInclusive;
  }

  public boolean contains(String version) {
    Semver current = new Semver(version, LOOSE);

    final boolean minWithin;
    if (isBlank(getLowerVersion())) {
      minWithin = true;
    } else {
      final Semver minMuleVersion = new Semver(getLowerVersion(), LOOSE);
      minWithin = isLowerBoundInclusive()
          ? minMuleVersion.isLowerThanOrEqualTo(current)
          : minMuleVersion.isLowerThan(current);
    }

    final boolean maxWithin;
    if (isBlank(getUpperVersion())) {
      maxWithin = true;
    } else {
      final Semver maxMuleVersion = new Semver(getUpperVersion(), LOOSE);
      maxWithin = isUpperBoundInclusive()
          ? maxMuleVersion.isGreaterThanOrEqualTo(current)
          : maxMuleVersion.isGreaterThan(current);
    }

    return minWithin && maxWithin;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(isLowerBoundInclusive() ? LOWER_BOUND_INCLUSIVE : LOWER_BOUND_EXCLUSIVE);
    sb.append(getLowerVersion());
    sb.append(",");
    sb.append(getUpperVersion());
    sb.append(isUpperBoundInclusive() ? UPPER_BOUND_INCLUSIVE : UPPER_BOUND_EXCLUSIVE);
    return sb.toString();
  }
}
