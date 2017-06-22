/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.apache.commons.lang3.SystemUtils.JAVA_VENDOR;
import org.mule.runtime.core.api.config.MuleManifest;
import org.mule.runtime.core.api.util.SystemUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdkVersionUtils {

  public static final String JAVA_VERSION_PROPERTY = "java.version";

  public static class JdkVersion implements Comparable<JdkVersion> {

    private Integer major;
    private Integer minor;
    private Integer micro;
    private Integer update;
    private String milestone;

    public JdkVersion(String jdkVersionStr) {
      Matcher m = JDK_VERSION.matcher(jdkVersionStr);
      if (m.matches()) {
        int numGroups = m.groupCount();
        if (numGroups >= 1 && m.group(1) != null && !m.group(1).isEmpty()) {
          major = Integer.parseInt(m.group(1));
        }
        if (numGroups >= 2 && m.group(2) != null && !m.group(2).isEmpty()) {
          minor = Integer.parseInt(m.group(2));
        }
        if (numGroups >= 3 && m.group(3) != null && !m.group(3).isEmpty()) {
          micro = Integer.parseInt(m.group(3));
        }
        if (numGroups >= 4 && m.group(4) != null && !m.group(4).isEmpty()) {
          update = Integer.parseInt(m.group(4));
        }
        if (numGroups >= 5 && m.group(5) != null && !m.group(5).isEmpty()) {
          milestone = m.group(5);
        }
      }
    }

    public Integer getMajor() {
      return major;
    }

    public Integer getMicro() {
      return micro;
    }

    public String getMilestone() {
      return milestone;
    }

    public Integer getMinor() {
      return minor;
    }

    public Integer getUpdate() {
      return update;
    }

    @Override
    public int compareTo(JdkVersion other) {
      int comparison = comparePointVersion(getMajor(), other.getMajor());
      if (comparison == 0) {
        comparison = comparePointVersion(getMinor(), other.getMinor());
        if (comparison == 0) {
          comparison = comparePointVersion(getMicro(), other.getMicro());
          if (comparison == 0) {
            comparison = comparePointVersion(getUpdate(), other.getUpdate());
            if (comparison == 0) {
              comparison = comparePointVersion(getMilestone(), other.getMilestone());
            }
          }
        }
      }
      return comparison;
    }

    private <T extends Comparable<T>> int comparePointVersion(T first, T second) {
      if (first != null && second != null) {
        return first.compareTo(second);
      } else if (first != null) {
        return 1;
      } else if (second != null) {
        return -1;
      } else {
        return 0;
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((major == null) ? 0 : major.hashCode());
      result = prime * result + ((micro == null) ? 0 : micro.hashCode());
      result = prime * result + ((milestone == null) ? 0 : milestone.hashCode());
      result = prime * result + ((minor == null) ? 0 : minor.hashCode());
      result = prime * result + ((update == null) ? 0 : update.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof JdkVersion))
        return false;
      JdkVersion other = (JdkVersion) obj;
      if (major == null) {
        if (other.major != null)
          return false;
      } else if (!major.equals(other.major))
        return false;
      if (micro == null) {
        if (other.micro != null)
          return false;
      } else if (!micro.equals(other.micro))
        return false;
      if (milestone == null) {
        if (other.milestone != null)
          return false;
      } else if (!milestone.equals(other.milestone))
        return false;
      if (minor == null) {
        if (other.minor != null)
          return false;
      } else if (!minor.equals(other.minor))
        return false;
      if (update == null) {
        if (other.update != null)
          return false;
      } else if (!update.equals(other.update))
        return false;
      return true;
    }


  }

  public static class JdkVersionRange extends VersionRange {

    private JdkVersion lower;
    private JdkVersion upper;

    public JdkVersionRange(String versionRange) {
      super(versionRange);
      if (!getLowerVersion().isEmpty()) {
        lower = new JdkVersion(getLowerVersion());
      }
      if (!getUpperVersion().isEmpty()) {
        upper = new JdkVersion(getUpperVersion());
      }
    }

    public boolean contains(JdkVersion jdkVersion) {
      return (lower == null || jdkVersion.compareTo(lower) > 0 || (jdkVersion.compareTo(lower) == 0 && isLowerBoundInclusive()))
          && (upper == null || jdkVersion.compareTo(upper) < 0 || (jdkVersion.compareTo(upper) == 0 && isUpperBoundInclusive()));
    }

    public boolean isUnder(JdkVersion jdkVersion) {
      return (upper != null)
          && (jdkVersion.compareTo(upper) > 0 || (jdkVersion.compareTo(upper) == 0 && !isUpperBoundInclusive()));
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(JdkVersionUtils.class);

  /**
   * pattern with groups for major, minor, micro, update and milestone (if exists).
   * major_version.minor_version.micro_version[_update_version][-milestone]
   */
  public static final Pattern JDK_VERSION =
      Pattern.compile("^([0-9]+)(?:\\.([0-9]+))?(?:\\.([0-9]+))?(?:_([0-9]+))?(?:-?(.+))?$");

  public static List<JdkVersionRange> createJdkVersionRanges(String versionsString) {
    Matcher m = VersionRange.VERSION_RANGES.matcher(versionsString);
    if (!m.find()) {
      throw new IllegalArgumentException("Version range doesn't match pattern: " + VersionRange.VERSION_RANGES.pattern());
    }

    List<JdkVersionRange> versions = new ArrayList<JdkVersionRange>();
    do {
      versions.add(new JdkVersionRange(m.group(1)));
    } while (m.find());

    return versions;
  }

  public static JdkVersion getJdkVersion() {
    return new JdkVersion(System.getProperty(JAVA_VERSION_PROPERTY));
  }

  public static String getSupportedJdks() {
    return MuleManifest.getSupportedJdks();
  }

  public static boolean isSupportedJdkVendor() {
    return SystemUtils.isSunJDK() || SystemUtils.isAppleJDK() || SystemUtils.isIbmJDK();
  }

  public static String getRecommendedJdks() {
    return MuleManifest.getRecommndedJdks();
  }

  public static boolean isSupportedJdkVersion() {
    boolean isSupported = true;
    String supportedJdks = getSupportedJdks();
    if (supportedJdks != null && !supportedJdks.isEmpty()) {
      List<JdkVersionRange> supportedJdkVersionRanges = createJdkVersionRanges(supportedJdks);
      isSupported = isJdkInRange(getJdkVersion(), supportedJdkVersionRanges);
    }
    return isSupported;
  }

  public static boolean isRecommendedJdkVersion() {
    boolean isRecommended = true;
    String recommendedJdks = getRecommendedJdks();
    if (recommendedJdks != null && !recommendedJdks.isEmpty()) {
      List<JdkVersionRange> recommendedJdkVersionRanges = createJdkVersionRanges(recommendedJdks);
      isRecommended = isJdkInRange(getJdkVersion(), recommendedJdkVersionRanges);
    }
    return isRecommended;
  }

  private static boolean isJdkInRange(JdkVersion version, List<JdkVersionRange> ranges) {
    for (JdkVersionRange versionRange : ranges) {
      if (versionRange.contains(version)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isJdkAboveRange(JdkVersion version, List<JdkVersionRange> ranges) {
    boolean isHigher = true;
    for (JdkVersionRange versionRange : ranges) {
      isHigher = isHigher && (versionRange.isUnder(version));
    }
    return isHigher;
  }

  /**
   * Validates that the jdk version and vendor are acceptable values (either supported or not invalid).
   * 
   * @throws RuntimeException if the jdk vendor or version are invalid (known to not work)
   */
  public static void validateJdk() throws RuntimeException {
    if (!isSupportedJdkVersion()) {
      if (isJdkAboveRange(getJdkVersion(), createJdkVersionRanges(getSupportedJdks()))) {
        logger.warn("We are looking into adding support for this JDK version. Use it at your own risk.");
      } else {
        throw new RuntimeException("Unsupported Jdk");
      }
    }
    if (!isSupportedJdkVendor()) {
      logger.info("You're executing with a JDK made by a vendor that is not on the recommended list of vendors. Vendor: "
          + JAVA_VENDOR + " Please consider changing to a recommended JDK vendor.");
    }
  }
}
