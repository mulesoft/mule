/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader.model.utils;

import com.vdurmont.semver4j.Semver;

/**
 * Helper methods to work with semantic versioning.
 */
public class VersionUtils {

  /**
   * Returns the version major.
   *
   * @param version
   * @return the major part of the version.
   */
  public static String getMajor(String version) {
    return String.valueOf(new Semver(version).getMajor());
  }

}
