/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.descriptor;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

import java.net.URL;
import java.util.Set;

/**
 * Contains all the information needed to create a {@link ClassLoader} for a Mule artifact.
 *
 * @deprecated since 4.5 use {@link ClassLoaderConfiguration} instead.
 */
@Deprecated
public final class ClassLoaderModel extends ClassLoaderConfiguration {

  /**
   * Defines a {@link ClassLoaderModel} with empty configuration
   */
  public static final ClassLoaderModel NULL_CLASSLOADER_MODEL =
      new ClassLoaderModel(new URL[0], emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), false);

  private ClassLoaderModel(URL[] urls, Set<String> exportedPackages, Set<String> exportedResources,
                           Set<String> localPackages, Set<String> localResources,
                           Set<BundleDependency> dependencies, Set<String> privilegedExportedPackages,
                           Set<String> privilegedArtifacts, boolean includeTestDependencies) {
    super(urls, exportedPackages, exportedResources, localPackages, localResources, dependencies, privilegedExportedPackages, privilegedArtifacts, includeTestDependencies);
  }

  public static ClassLoaderModel fromClassLoaderConfiguration(ClassLoaderConfiguration classLoaderConfiguration) {
    return new ClassLoaderModel(classLoaderConfiguration.getUrls(), classLoaderConfiguration.getExportedPackages(), classLoaderConfiguration.getExportedResources(), classLoaderConfiguration.getLocalPackages(), classLoaderConfiguration.getLocalResources(), classLoaderConfiguration.getDependencies(), classLoaderConfiguration.getPrivilegedExportedPackages(), classLoaderConfiguration.getPrivilegedArtifacts(), classLoaderConfiguration.isIncludeTestDependencies());
  }

  /**
   * Builds a {@link ClassLoaderModel}
   *
   * @deprecated since 4.5 use {@link ClassLoaderConfigurationBuilder} instead.
   */
  @Deprecated
  public static class ClassLoaderModelBuilder extends ClassLoaderConfigurationBuilder {

    @Override
    public ClassLoaderModel doBuild() {
      return new ClassLoaderModel(getUrls().toArray(new URL[0]),
              unmodifiableSet(getPackages()), unmodifiableSet(getResources()),
              unmodifiableSet(getLocalPackages()), unmodifiableSet(getLocalResources()),
              unmodifiableSet(dependencies),
              unmodifiableSet(getPrivilegedExportedPackages()), unmodifiableSet(getPrivilegedArtifacts()),
              getIncludeTestDependencies());
    }
  }
}
