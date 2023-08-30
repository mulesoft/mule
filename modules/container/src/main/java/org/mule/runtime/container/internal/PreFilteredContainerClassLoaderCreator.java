/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.internal;

import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Creates a container class loader without adding a filter to it.
 *
 * @since 4.5
 */
public interface PreFilteredContainerClassLoaderCreator {

  /**
   * Boot packages define all the prefixes that must be loaded from the container classLoader without being filtered
   */
  Set<String> BOOT_PACKAGES =
      ImmutableSet.of(// MULE-10194 Mechanism to add custom boot packages to be exported by the container
                      "com.yourkit");

  /**
   * @return the list of {@link MuleModule}s to be used for defining the filter
   */
  List<MuleModule> getMuleModules();

  /**
   * @return a {@link Set} of packages that define all the prefixes that must be loaded from the container classLoader without
   *         being filtered
   */
  Set<String> getBootPackages();

  /**
   * @param artifactDescriptor descriptor for the artifact owning the created class loader instance.
   * @param parentClassLoader  the parent {@link ClassLoader} for the container.
   * @return the container class loader without a filter added.
   */
  ArtifactClassLoader getPreFilteredContainerClassLoader(ArtifactDescriptor artifactDescriptor, ClassLoader parentClassLoader);
}
