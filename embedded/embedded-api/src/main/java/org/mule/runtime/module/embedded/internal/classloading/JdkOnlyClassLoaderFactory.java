/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.embedded.internal.classloading;

import static org.mule.runtime.module.embedded.internal.classloading.JreExplorer.exploreJdk;

import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// TODO MULE-11882 - Consolidate classloading isolation
public class JdkOnlyClassLoaderFactory {

  public static final Set<String> BOOT_PACKAGES =
      ImmutableSet.of("org.apache.xerces",
                      "org.apache.logging.log4j", "org.slf4j", "org.apache.commons.logging", "org.apache.log4j",
                      "com.yourkit");

  public static FilteringClassLoader create() {
    Set<String> packages = new HashSet<>(1024);
    Set<String> resources = new HashSet<>(1024);
    List<ExportedService> services = new ArrayList<>(128);
    exploreJdk(packages, resources, services);

    ClassLoaderFilter classLoaderFilter =
        new ClassLoaderFilter(ImmutableSet.<String>builder().addAll(BOOT_PACKAGES).addAll(packages).build());

    return new FilteringClassLoader(classLoaderFilter, services);
  }
}
