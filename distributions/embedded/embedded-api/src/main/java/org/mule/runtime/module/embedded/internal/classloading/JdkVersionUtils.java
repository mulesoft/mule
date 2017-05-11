/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.embedded.internal.classloading;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO MULE-11882 - Consolidate classloading isolation
public class JdkVersionUtils {

  public static final String JAVA_VERSION_PROPERTY = "java.version";

  /**
   * pattern with groups for major, minor, micro, update and milestone (if exists).
   * major_version.minor_version.micro_version[_update_version][-milestone]
   */
  public static final Pattern JDK_VERSION =
      Pattern.compile("^([0-9]+)(?:\\.([0-9]+))?(?:\\.([0-9]+))?(?:_([0-9]+))?(?:-?(.+))?$");

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

}
