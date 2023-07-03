/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.api;

import static org.mule.runtime.container.internal.ContainerClassLoaderCreatorUtils.SYSTEM_PACKAGES;

import org.mule.runtime.container.internal.DefaultTestContainerClassLoaderAssembler;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Test utility that allows to manage the container class loader for Mule functional test cases.
 *
 * @since 4.5
 */
public interface TestContainerClassLoaderAssembler extends AutoCloseable {

  static TestContainerClassLoaderAssembler create(List<String> extraBootPackages, Set<String> extraPrivilegedArtifacts,
                                                  List<URL> urls) {
    return new DefaultTestContainerClassLoaderAssembler(extraBootPackages, extraPrivilegedArtifacts, urls);
  }

  /**
   * @return a collection with the system packages.
   */
  static Collection<String> getSystemPackages() {
    return SYSTEM_PACKAGES;
  }

  /**
   * @return the container class loader with all the configuration needed for the tests.
   */
  MuleContainerClassLoaderWrapper createContainerClassLoader();

  /**
   * @return a lookup strategy that allows searching for a class only in the container class loader.
   */
  LookupStrategy getContainerOnlyLookupStrategy();

  /**
   * @return the module repository used for creating the container class loader.
   */
  ModuleRepository getModuleRepository();

}
