/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.embedded.internal.classloading;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE;

import java.util.Arrays;
import java.util.Set;

/**
 * Determines if a given class or resource is exported in a plugin classloader
 */
// TODO MULE-11882 - Consolidate classloading isolation
public class ClassLoaderFilter {

  private static final String EMPTY_PACKAGE = "";
  private static final char RESOURCE_SEPARATOR = '/';
  public static final String CLASS_PACKAGE_SPLIT_REGEX = "\\.";
  public static final String RESOURCE_PACKAGE_SPLIT_REGEX = "/";
  private final Set<String> bootPackages;

  public ClassLoaderFilter(Set<String> bootPackages) {
    this.bootPackages = bootPackages;
  }

  public boolean exportsClass(String name) {
    return isExportedBooPackage(name, CLASS_PACKAGE_SPLIT_REGEX);
  }

  public boolean exportsResource(String name) {
    return isExportedBooPackage(name, RESOURCE_PACKAGE_SPLIT_REGEX);
  }

  private boolean isExportedBooPackage(String name, String splitRegex) {
    boolean exported = false;
    final String[] splitName = name.split(splitRegex);
    final String[] packages = Arrays.copyOf(splitName, splitName.length - 1);
    String candidatePackage = "";

    for (String currentPackage : packages) {
      if (candidatePackage.length() != 0) {
        candidatePackage += ".";
      }
      candidatePackage += currentPackage;

      if (bootPackages.contains(candidatePackage)) {
        exported = true;
        break;
      }

    }
    return exported;
  }

  @Override
  public String toString() {
    return reflectionToString(this, MULTI_LINE_STYLE);
  }
}
