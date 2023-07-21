/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
